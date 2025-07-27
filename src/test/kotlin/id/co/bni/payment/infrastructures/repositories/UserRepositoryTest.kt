package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.domains.entities.User
import id.co.bni.payment.infrastructures.repositories.dao.UserDao
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class UserRepositoryTest {

    @MockK
    private lateinit var userDAO: UserDao

    @InjectMockKs
    private lateinit var userRepository: UserRepositoryImpl

    @Test
    fun `findByUsername should return user when found`() = runTest {
        // arrange
        val user = User(
            id = 2L,
            username = "jane",
            phone = "987654321",
            email = "jane@example.com",
            password = "password456"
        )
        coEvery { userDAO.findByUsername("jane") } returns user

        // act
        val result = userRepository.findByUsername("jane")

        // assert
        assertEquals(2L, result?.id)
        assertEquals("jane", result?.username)
        assertEquals("jane@example.com", result?.email)
    }

    @Test
    fun `findByUsername should return null when not found`() = runTest {
        // arrange
        coEvery { userDAO.findByUsername("notfound") } returns null

        // act
        val result = userRepository.findByUsername("notfound")

        // assert
        assertNull(result)
    }
}