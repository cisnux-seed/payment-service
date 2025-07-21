package id.co.bni.payment.domains.repositories

import id.co.bni.payment.domains.entities.User

interface UserRepository {
    suspend fun findByUsername(username: String): User?
}