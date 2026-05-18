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
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            snapshot?.toObject(User::class.java)?.let { user ->
                _currentUser.value = user
                startListeningToData(user.id)
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
                val NotFoundEmails = mutableListOf<String>()
                
                for (email in memberEmails) {
                    val cleanEmail = email.trim().lowercase()
                    if (cleanEmail.isNotBlank()) {
                        val snapshot = db.collection("users")
                            .whereEqualTo("email", cleanEmail)
                            .get().await()
                        if (!snapshot.isEmpty) {
                            val userId = snapshot.documents[0].id
                            if (!memberIds.contains(userId)) {
                                memberIds.add(userId)
                                initialBalances[userId] = 0.0
                            }
                        } else {
                            NotFoundEmails.add(email)
                        }
                    }
                }

                if (NotFoundEmails.isNotEmpty()) {
                    _isLoading.value = false
                    onError("Could not find users with these emails: ${NotFoundEmails.joinToString(", ")}. Please make sure they have signed up for Splitify.")
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
                val snapshot = db.collection("users")
                    .whereEqualTo("email", email.trim().lowercase())
                    .get().await()
                
                if (snapshot.isEmpty) {
                    onError("User not found with this email")
                    return@launch
                }

                val userId = snapshot.documents[0].id
                val groupRef = db.collection("groups").document(groupId)

                db.runTransaction { transaction ->
                    val groupDoc = transaction.get(groupRef)
                    val members = groupDoc.get("members") as? List<String> ?: emptyList()
                    val balances = groupDoc.get("balances") as? Map<String, Any> ?: emptyMap()

                    if (members.contains(userId)) {
                        throw Exception("User is already a member of this group")
                    }

                    val newMembers = members + userId
                    val newBalances = balances.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }.toMutableMap()
                    newBalances[userId] = 0.0

                    transaction.update(groupRef, "members", newMembers)
                    transaction.update(groupRef, "balances", newBalances)
                }.await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add member")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addExpense(
        description: String,
        amount: Double,
        paidBy: String, 
        groupId: String?,
        involvedUserIds: List<String>
    ) {
        val currentUserId = _currentUser.value?.id ?: return
        val payerId = if (paidBy == "You") currentUserId else paidBy
        
        val expense = Expense(
            description = description,
            amount = amount,
            paidBy = payerId,
            involvedUserIds = involvedUserIds,
            groupId = groupId,
            date = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.runTransaction { transaction ->
                    var updatedBalances: Map<String, Double>? = null
                    
                    if (groupId != null) {
                        val groupRef = db.collection("groups").document(groupId)
                        val groupDoc = transaction.get(groupRef)
                        
                        val currentBalances = groupDoc.get("balances") as? Map<String, Any> ?: emptyMap()
                        val mUpdatedBalances = currentBalances.mapValues { 
                            (it.value as? Number)?.toDouble() ?: 0.0 
                        }.toMutableMap()
                        
                        val share = if (involvedUserIds.isNotEmpty()) amount / involvedUserIds.size else 0.0
                        val isPayerInvolved = involvedUserIds.contains(payerId)
                        val payerCredit = if (isPayerInvolved) amount - share else amount
                        
                        mUpdatedBalances[payerId] = (mUpdatedBalances[payerId] ?: 0.0) + payerCredit
                        
                        involvedUserIds.forEach { userId ->
                            if (userId != payerId) {
                                mUpdatedBalances[userId] = (mUpdatedBalances[userId] ?: 0.0) - share
                            }
                        }
                        updatedBalances = mUpdatedBalances
                    }

                    val expenseRef = db.collection("expenses").document()
                    transaction.set(expenseRef, expense.copy(id = expenseRef.id))
                    
                    if (groupId != null && updatedBalances != null) {
                        val groupRef = db.collection("groups").document(groupId)
                        transaction.update(groupRef, "balances", updatedBalances)
                    }
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
                        val updatedBalances = currentBalances.mapValues { 
                            (it.value as? Number)?.toDouble() ?: 0.0 
                        }.toMutableMap()
                        
                        val share = if (expense.involvedUserIds.isNotEmpty()) expense.amount / expense.involvedUserIds.size else 0.0
                        val payerId = expense.paidBy
                        
                        val isPayerInvolved = expense.involvedUserIds.contains(payerId)
                        val payerCredit = if (isPayerInvolved) expense.amount - share else expense.amount
                        updatedBalances[payerId] = (updatedBalances[payerId] ?: 0.0) - payerCredit
                        
                        expense.involvedUserIds.forEach { userId ->
                            if (userId != payerId) {
                                updatedBalances[userId] = (updatedBalances[userId] ?: 0.0) + share
                            }
                        }
                        transaction.update(groupRef, "balances", updatedBalances)
                    }

                    val expenseRef = db.collection("expenses").document(expense.id)
                    transaction.delete(expenseRef)
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun settleGroup(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val groupRef = db.collection("groups").document(groupId)
                val doc = groupRef.get().await()
                val balances = doc.get("balances") as? Map<String, Any> ?: return@launch
                val updatedBalances = balances.mapValues { 0.0 }
                groupRef.update("balances", updatedBalances).await()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
