package com.example.splitify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitify.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val totalYouAreOwed = MutableStateFlow(0.0)
    val totalYouOwe = MutableStateFlow(0.0)

    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            fetchCurrentUserData(firebaseUser.uid)
        } else {
            _currentUser.value = null
            _friends.value = emptyList()
            _groups.value = emptyList()
            _expenses.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            onError("Email and password cannot be empty")
            return
        }
        _isLoading.value = true
        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { 
                _isLoading.value = false
                onSuccess() 
            }
            .addOnFailureListener { error ->
                _isLoading.value = false
                val message = when {
                    error.message?.contains("password") == true -> "Incorrect password. Please try again."
                    error.message?.contains("no user") == true || error.message?.contains("not found") == true -> 
                        "No account found with this email. Please Sign Up first."
                    error.message?.contains("malformed") == true || error.message?.contains("credential") == true ->
                        "Invalid credentials. Please check your email/password."
                    else -> error.localizedMessage ?: "Sign in failed"
                }
                onError(message)
            }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener
                val user = User(id = userId, name = name.trim(), email = cleanEmail)
                
                db.collection("users").document(userId).set(user)
                    .addOnSuccessListener { 
                        _isLoading.value = false
                        onSuccess() 
                    }
                    .addOnFailureListener { error ->
                        _isLoading.value = false
                        onError("Auth successful, but profile creation failed: ${error.localizedMessage}")
                    }
            }
            .addOnFailureListener { error ->
                _isLoading.value = false
                onError(error.localizedMessage ?: "Sign up failed")
            }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    private fun fetchCurrentUserData(userId: String) {
        db.collection("users").document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("Splitify", "Error fetching user data", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    _currentUser.value = user
                    startListeningToData(user.id)
                }
            } else {
                // If auth exists but no Firestore doc, create a basic one from Auth profile
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val newUser = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: ""
                    )
                    db.collection("users").document(firebaseUser.uid).set(newUser)
                        .addOnSuccessListener {
                            _currentUser.value = newUser
                            startListeningToData(firebaseUser.uid)
                        }
                }
            }
        }
    }

    private fun startListeningToData(userId: String) {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            val users = snapshot?.toObjects(User::class.java) ?: emptyList()
            _usersMap.value = users.associateBy { it.id }
        }

        db.collection("groups")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, _ ->
                val groupList = snapshot?.toObjects(Group::class.java) ?: emptyList()
                _groups.value = groupList
                calculateAggregates(groupList, _expenses.value)
                updateFriendsList(userId, groupList)
            }

        db.collection("expenses")
            .where(
                Filter.or(
                    Filter.arrayContains("involvedUserIds", userId),
                    Filter.equalTo("paidBy", userId)
                )
            )
            .addSnapshotListener { snapshot, error ->
                val expenseList = if (error != null) {
                    emptyList() 
                } else {
                    snapshot?.toObjects(Expense::class.java)?.sortedByDescending { it.date } ?: emptyList()
                }
                _expenses.value = expenseList
                calculateAggregates(_groups.value, expenseList)
            }
    }

    private fun updateFriendsList(currentUserId: String, groups: List<Group>) {
        db.collection("users").get().addOnSuccessListener { snapshot ->
            val allUsers = snapshot?.toObjects(User::class.java) ?: emptyList()
            val friendBalances = mutableMapOf<String, Double>()
            
            groups.forEach { group ->
                group.members.forEach { memberId ->
                    if (memberId != currentUserId) {
                        val currentFriendBalance = friendBalances[memberId] ?: 0.0
                        friendBalances[memberId] = currentFriendBalance + (group.balances[memberId] ?: 0.0) * -1
                    }
                }
            }

            _friends.value = allUsers
                .filter { it.id != currentUserId && friendBalances.containsKey(it.id) }
                .map { user ->
                    Friend(user = user, balanceWithMe = friendBalances[user.id] ?: 0.0)
                }
        }
    }

    fun addFriend(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUserId = _currentUser.value?.id ?: return
        val cleanEmail = email.trim().lowercase()
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .get().await()
                
                if (snapshot.isEmpty) {
                    onError("User not found")
                    return@launch
                }

                val friendUser = snapshot.toObjects(User::class.java)[0]
                if (friendUser.id == currentUserId) {
                    onError("You cannot add yourself as a friend")
                    return@launch
                }

                if (_friends.value.any { it.user.id == friendUser.id }) {
                    onError("User is already your friend")
                    return@launch
                }

                val friendData = mapOf(
                    "friendId" to friendUser.id,
                    "addedAt" to System.currentTimeMillis()
                )
                
                db.collection("users").document(currentUserId)
                    .collection("friends").document(friendUser.id)
                    .set(friendData).await()
                
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add friend")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateAggregates(groups: List<Group>, expenses: List<Expense>) {
        val currentUserId = _currentUser.value?.id ?: return
        var owed = 0.0
        var owe = 0.0
        
        groups.forEach { group ->
            val userBalance = group.balances[currentUserId] ?: 0.0
            if (userBalance > 0) owed += userBalance
            else if (userBalance < 0) owe += -userBalance
        }
        
        totalYouAreOwed.value = owed
        totalYouOwe.value = owe
    }

    fun createGroup(name: String, memberEmails: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUserId = _currentUser.value?.id ?: run {
            onError("You must be logged in to create a group.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val memberIds = mutableListOf(currentUserId)
                val initialBalances = mutableMapOf(currentUserId to 0.0)
                val notFoundEmails = mutableListOf<String>()
                
                for (email in memberEmails) {
                    val cleanEmail = email.trim().lowercase()
                    if (cleanEmail.isNotBlank()) {
                        val snapshot = db.collection("users")
                            .whereEqualTo("email", cleanEmail)
                            .get().await()
                        
                        val userId = if (!snapshot.isEmpty) {
                            snapshot.documents[0].id
                        } else {
                            _usersMap.value.values.find { it.email.trim().lowercase() == cleanEmail }?.id
                        }

                        if (userId != null) {
                            if (!memberIds.contains(userId)) {
                                memberIds.add(userId)
                                initialBalances[userId] = 0.0
                            }
                        } else {
                            notFoundEmails.add(email)
                        }
                    }
                }

                if (notFoundEmails.isNotEmpty()) {
                    _isLoading.value = false
                    onError("Could not find users: ${notFoundEmails.joinToString(", ")}. They must sign up first.")
                    return@launch
                }

                val group = Group(name = name, members = memberIds, balances = initialBalances)
                db.collection("groups").add(group).await()
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError(e.localizedMessage ?: "Failed to create group")
            }
        }
    }

    fun addMemberToGroup(groupId: String, email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cleanEmail = email.trim().lowercase()
                
                // First, check if the user is already in our usersMap (already a friend or known)
                var userId = _usersMap.value.values.find { it.email.trim().lowercase() == cleanEmail }?.id
                
                // If not in usersMap, look up in Firestore
                if (userId == null) {
                    val snapshot = db.collection("users")
                        .whereEqualTo("email", cleanEmail)
                        .get().await()
                    
                    if (!snapshot.isEmpty) {
                        userId = snapshot.documents[0].id
                        // Add to usersMap if not present
                        val user = snapshot.documents[0].toObject(User::class.java)
                        if (user != null) {
                            val currentMap = _usersMap.value.toMutableMap()
                            currentMap[user.id] = user
                            _usersMap.value = currentMap
                        }
                    }
                }
                
                if (userId == null) {
                    _isLoading.value = false
                    onError("User not found with email: $email")
                    return@launch
                }

                val targetUserId = userId
                val groupRef = db.collection("groups").document(groupId)
                db.runTransaction { transaction ->
                    val groupDoc = transaction.get(groupRef)
                    val members = groupDoc.get("members") as? List<String> ?: emptyList()
                    val balances = groupDoc.get("balances") as? Map<String, Any> ?: emptyMap()

                    if (members.contains(targetUserId)) throw Exception("User is already in this group")

                    val newMembers = members + targetUserId
                    val newBalances = balances.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }.toMutableMap()
                    newBalances[targetUserId] = 0.0

                    transaction.update(groupRef, "members", newMembers)
                    transaction.update(groupRef, "balances", newBalances)
                    null
                }.await()
                
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError(e.message ?: "Failed to add member")
            }
        }
    }

    fun addExpense(description: String, amount: Double, paidBy: String, groupId: String?, involvedUserIds: List<String>) {
        val currentUserId = _currentUser.value?.id ?: return
        val expense = Expense(
            description = description,
            amount = amount,
            paidBy = paidBy,
            involvedUserIds = involvedUserIds,
            groupId = groupId,
            date = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.runTransaction { transaction ->
                    if (groupId != null) {
                        val groupRef = db.collection("groups").document(groupId)
                        val groupDoc = transaction.get(groupRef)
                        val currentBalances = groupDoc.get("balances") as? Map<String, Any> ?: emptyMap()
                        val mUpdatedBalances = currentBalances.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }.toMutableMap()
                        
                        val share = if (involvedUserIds.isNotEmpty()) amount / involvedUserIds.size else 0.0
                        
                        // Update Payer
                        mUpdatedBalances[paidBy] = (mUpdatedBalances[paidBy] ?: 0.0) + amount
                        
                        // Update Involved
                        involvedUserIds.forEach { userId ->
                            mUpdatedBalances[userId] = (mUpdatedBalances[userId] ?: 0.0) - share
                        }
                        
                        transaction.update(groupRef, "balances", mUpdatedBalances)
                    }

                    val expenseRef = db.collection("expenses").document()
                    transaction.set(expenseRef, expense.copy(id = expenseRef.id))
                    null
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.runTransaction { transaction ->
                    if (expense.groupId != null) {
                        val groupRef = db.collection("groups").document(expense.groupId)
                        val groupDoc = transaction.get(groupRef)
                        val currentBalances = groupDoc.get("balances") as? Map<String, Any> ?: emptyMap()
                        val updatedBalances = currentBalances.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }.toMutableMap()
                        
                        val share = if (expense.involvedUserIds.isNotEmpty()) expense.amount / expense.involvedUserIds.size else 0.0
                        
                        // Reverse Payer Credit
                        updatedBalances[expense.paidBy] = (updatedBalances[expense.paidBy] ?: 0.0) - expense.amount
                        
                        // Reverse Shared Debt
                        expense.involvedUserIds.forEach { userId ->
                            updatedBalances[userId] = (updatedBalances[userId] ?: 0.0) + share
                        }
                        transaction.update(groupRef, "balances", updatedBalances)
                    }
                    val expenseRef = db.collection("expenses").document(expense.id)
                    transaction.delete(expenseRef)
                    null
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun settleGroup(groupId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Calculate settlements before clearing balances
                val group = _groups.value.find { it.id == groupId } ?: throw Exception("Group not found")
                val calculatedSettlements = calculateSettlements(group.balances)
                _settlements.value = calculatedSettlements
                
                db.runTransaction { transaction ->
                    val groupRef = db.collection("groups").document(groupId)
                    val groupDoc = transaction.get(groupRef)
                    if (!groupDoc.exists()) throw Exception("Group not found")
                    
                    val balances = groupDoc.get("balances") as? Map<String, Any> ?: emptyMap()
                    val updatedBalances = balances.mapValues { 0.0 }
                    
                    transaction.update(groupRef, "balances", updatedBalances)
                    null
                }.await()
                
                // Mark existing expenses as settled and add settlement records
                val expensesSnapshot = db.collection("expenses")
                    .whereEqualTo("groupId", groupId)
                    .whereEqualTo("isSettled", false)
                    .get().await()
                
                val batch = db.batch()
                expensesSnapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isSettled", true)
                }

                // Add "Settlement" entries for history/activity
                calculatedSettlements.forEach { settlement ->
                    val settlementExpense = Expense(
                        description = "Settle Up",
                        amount = settlement.amount,
                        paidBy = settlement.fromUserId,
                        involvedUserIds = listOf(settlement.toUserId),
                        groupId = groupId,
                        date = System.currentTimeMillis(),
                        isSettled = true,
                        isSettlement = true
                    )
                    val newRef = db.collection("expenses").document()
                    batch.set(newRef, settlementExpense.copy(id = newRef.id))
                }

                batch.commit().await()

                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to settle balances")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val group = _groups.value.find { it.id == groupId } ?: throw Exception("Group not found")
                
                // Check if all balances are zero (within a small epsilon)
                val isSettled = group.balances.values.all { kotlin.math.abs(it) < 0.01 }
                
                if (!isSettled) {
                    throw Exception("Cannot delete group: All balances must be settled (zero) first.")
                }

                // Delete all expenses associated with this group
                val expensesSnapshot = db.collection("expenses").whereEqualTo("groupId", groupId).get().await()
                val batch = db.batch()
                expensesSnapshot.documents.forEach { batch.delete(it.reference) }
                
                // Delete the group document
                val groupRef = db.collection("groups").document(groupId)
                batch.delete(groupRef)
                
                batch.commit().await()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to delete group")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateSettlements(balances: Map<String, Double>): List<Settlement> {
        val debtors = balances.filter { it.value < -0.01 }.map { it.key to -it.value }.toMutableList()
        val creditors = balances.filter { it.value > 0.01 }.map { it.key to it.value }.toMutableList()
        val settlements = mutableListOf<Settlement>()

        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val (debtorId, debtAmount) = debtors[dIdx]
            val (creditorId, creditAmount) = creditors[cIdx]

            val settleAmount = minOf(debtAmount, creditAmount)
            if (settleAmount > 0) {
                settlements.add(Settlement(debtorId, creditorId, settleAmount))
            }

            debtors[dIdx] = debtorId to (debtAmount - settleAmount)
            creditors[cIdx] = creditorId to (creditAmount - settleAmount)

            if (debtors[dIdx].second < 0.01) dIdx++
            if (creditors[cIdx].second < 0.01) cIdx++
        }
        return settlements
    }
}
