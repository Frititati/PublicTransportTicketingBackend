package it.polito.wa2.payment.security

import io.jsonwebtoken.*
import it.polito.wa2.payment.dtos.UserLoggedDTO
import org.springframework.beans.factory.annotation.Value
import io.jsonwebtoken.security.*
import io.jsonwebtoken.security.SignatureException
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

@Component
class JwtUtils {

    @Value("\${application.loginKey}")
    lateinit var secretString: String

    fun validateJwt(authToken : String?) : Boolean {
        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))
        if(authToken.isNullOrEmpty()) return false
        else
        try {
            Jwts.parserBuilder().setSigningKey(generatedKey).build().parseClaimsJws(authToken)
            return true
        } catch (ex: SignatureException) {
            println("Invalid signature")
        } catch (ex: ExpiredJwtException) {
            println("JWT Expired")
        } catch (ex: MalformedJwtException) {
            println("Malformed JSON")
        } catch (ex: UnsupportedJwtException) {
            println("Unsupported token")
        } catch (ex: Exception) {
            println(ex.message)
        }
        return false
    }

    fun getDetailsJwt(authToken: String) : UserLoggedDTO? {
        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))
        return try {
            val u = Jwts.parserBuilder().setSigningKey(generatedKey).build().parseClaimsJws(authToken).body.subject
            val role = Jwts.parserBuilder().setSigningKey(generatedKey).build().parseClaimsJws(authToken).body["role"].toString()
            UserLoggedDTO(u, Role.valueOf(role))

        } catch (ex: Exception) {
            println(ex.message)
            null
        }
    }
}