package code.api.util

import code.api.RequestHeader
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.model.User
import net.liftweb.common.{Box, Failure, Full}
import net.liftweb.http.provider.HTTPParam

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security._
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64
import scala.util.matching.Regex

object BerlinGroupSigning extends MdcLoggable {

  // Step 1: Calculate Digest (SHA-256 Hash of the Body)
  def calculateDigest(body: String): String = {
    def removeFirstAndLastQuotes(input: String): String = {
      if (input.startsWith("\"") && input.endsWith("\"") && input.length > 1) {
        input.tail.init
      } else {
        input
      }
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(removeFirstAndLastQuotes(body).getBytes(StandardCharsets.UTF_8))
    Base64.getEncoder.encodeToString(digest)
  }

  // Step 2: Create Signing String (Concatenation of required headers)
  def createSigningString(headers: Map[String, String]): String = {
    // headers=”digest date x-request-id tpp-redirect-uri”
    val orderedKeys = List(
      RequestHeader.Digest,
      RequestHeader.Date,
      RequestHeader.`X-Request-ID`,
      //RequestHeader.`TPP-Redirect-URL`,
    ) // Example fields to be signed
    orderedKeys.flatMap(key => headers.get(key).map(value => s"${key.toLowerCase()}: $value")).mkString("\n")
  }

  // Step 3: Generate Signature using RSA Private Key
  def signString(signingString: String, privateKeyPem: String): String = {
    val privateKey = loadPrivateKey(privateKeyPem)
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initSign(privateKey)
    signature.update(signingString.getBytes(StandardCharsets.UTF_8))
    Base64.getEncoder.encodeToString(signature.sign())
  }

  // Load RSA Private Key from PEM String
  def loadPrivateKey(pem: String): PrivateKey = {
    val keyString = pem
      .replaceAll("-----BEGIN .*?PRIVATE KEY-----", "") // Remove headers
      .replaceAll("-----END .*?PRIVATE KEY-----", "")
      .replaceAll("\\s", "") // Remove all whitespace and new lines

    val decodedKey = Base64.getDecoder.decode(keyString)
    val keySpec = new PKCS8EncodedKeySpec(decodedKey)
    KeyFactory.getInstance("RSA").generatePrivate(keySpec)
  }

  // Step 4: Attach Certificate (Load from PEM String)
  def loadCertificate(certPem: String) = {
    val certString = certPem
      .replaceAll("-----BEGIN CERTIFICATE-----", "") // Remove the BEGIN header
      .replaceAll("-----END CERTIFICATE-----", "")   // Remove the END footer
      .replaceAll("\\s", "") // Remove all whitespace and new lines

    // Decode Base64 public key
    val keyBytes = Base64.getDecoder.decode(certPem)
    val keySpec = new X509EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance("RSA")
    val publicKey = keyFactory.generatePublic(keySpec)
    publicKey
    // val certBytes = Base64.getDecoder.decode(certString)
    // Base64.getEncoder.encodeToString(certBytes)
  }

  // Step 5: Verify Request on ASPSP Side
  def verifySignature(signingString: String, signatureStr: String, certPem: String): Boolean = {
    val publicKey = loadPublicKeyFromCert(certPem)
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initVerify(publicKey)
    signature.update(signingString.getBytes(StandardCharsets.UTF_8))
    signature.verify(Base64.getDecoder.decode(signatureStr))
  }

  // Extract Public Key from PEM Certificate String
  def loadPublicKeyFromCert(certPem: String): PublicKey = {
    val certString = certPem
      .replaceAll("-----BEGIN CERTIFICATE-----", "") // Remove the BEGIN header
      .replaceAll("-----END CERTIFICATE-----", "")   // Remove the END footer
      .replaceAll("\\s", "") // Remove all whitespace and new lines

    val certBytes = Base64.getDecoder.decode(certString)
    val certFactory = CertificateFactory.getInstance("X.509")
    val cert = certFactory.generateCertificate(new java.io.ByteArrayInputStream(certBytes)).asInstanceOf[X509Certificate]
    cert.getPublicKey
  }


  /**
   * Verifies Signed Request. It assumes that Customers has a sored certificate.
   *
   * @param body          of the signed request
   * @param verb          GET, POST, DELETE, etc.
   * @param url           of the the signed request. For example: /berlin-group/v1.3/payments/sepa-credit-transfers
   * @param reqHeaders    All request headers of the signed request
   * @param forwardResult Propagated result of calling function
   * @return Propagated result of calling function or signing request error
   */
  def verifySignedRequest(body: Box[String], verb: String, url: String, reqHeaders: List[HTTPParam], forwardResult: (Box[User], Option[CallContext])): (Box[User], Option[CallContext]) = {
    def checkRequestIsSigned(requestHeaders: List[HTTPParam]): Boolean = {
      requestHeaders.exists(_.name == RequestHeader.`TPP-Signature-Certificate`) &&
      requestHeaders.exists(_.name == RequestHeader.Signature) &&
        requestHeaders.exists(_.name == RequestHeader.Digest)
    }
    checkRequestIsSigned(forwardResult._2.map(_.requestHeaders).getOrElse(Nil)) match {
      case false =>
        forwardResult
      case true =>
        val requestHeaders = forwardResult._2.map(_.requestHeaders).getOrElse(Nil)
        val certificatePem: String = getPem(requestHeaders)
        X509.validate(certificatePem) match {
          case Full(true) => // PEM certificate is ok
            val digest = calculateDigest(body.getOrElse(""))


            val signatureHeaderValue = getHeaderValue(RequestHeader.Signature, requestHeaders)
            val signature = parseSignatureHeader(signatureHeaderValue).getOrElse("signature", "NONE")
            val headersToSign = parseSignatureHeader(signatureHeaderValue).getOrElse("headers", "").split(" ").toList
            val headers = headersToSign.map(h =>
              if(h.toLowerCase() == RequestHeader.Digest.toLowerCase()) {
                s"$h: SHA-256=$digest"
              } else {
                s"$h: ${getHeaderValue(h, requestHeaders)}"
              }
            )
            val signingString = headers.mkString("\n")
            val isVerified = verifySignature(signingString, signature, certificatePem)
            val isValidated = CertificateVerifier.validateCertificate(certificatePem)
            val bypassValidation = APIUtil.getPropsAsBoolValue("bypass_tpp_signature_validation", defaultValue = false)
            (isVerified, isValidated) match {
              case (true, true) => forwardResult
              case (true, false) if bypassValidation => forwardResult
              case (true, false) => (Failure(ErrorMessages.X509PublicKeyCannotBeValidated), forwardResult._2)
              case (false, _) => (Failure(ErrorMessages.X509PublicKeyCannotVerify), forwardResult._2)
            }
          case Failure(msg, t, c) => (Failure(msg, t, c), forwardResult._2) // PEM certificate is not valid
          case _ => (Failure(ErrorMessages.X509GeneralError), forwardResult._2) // PEM certificate cannot be validated
        }
    }
  }

  def getHeaderValue(name: String, requestHeaders: List[HTTPParam]): String = {
    requestHeaders.find(_.name.toLowerCase() == name.toLowerCase()).map(_.values.mkString).getOrElse("None")
  }
  private def getPem(requestHeaders: List[HTTPParam]): String = {
    val certificate = getHeaderValue(RequestHeader.`TPP-Signature-Certificate`, requestHeaders)
    // Decode the Base64 string
    val decodedBytes = Base64.getDecoder.decode(certificate)
    // Convert the bytes to a string (it could be PEM format for public key)
    val decodedString = new String(decodedBytes, StandardCharsets.UTF_8)

    // Extract the certificate portion from the decoded string
    val certStart = "-----BEGIN CERTIFICATE-----"
    val certEnd = "-----END CERTIFICATE-----"

    // Find the start and end indices of the certificate
    val startIndex = decodedString.indexOf(certStart)
    val endIndex = decodedString.indexOf(certEnd, startIndex) + certEnd.length

    if (startIndex >= 0 && endIndex >= 0) {
      // Extract and print the certificate part
      val extractedCert = decodedString.substring(startIndex, endIndex)
      logger.debug("Extracted Certificate:")
      logger.debug(extractedCert)
      extractedCert
    } else {
      logger.debug("Certificate not found in the decoded string.")
      ""
    }
  }

  def getTppSignatureCertificate(requestHeaders: List[HTTPParam]): Option[String] = {
    getPem(requestHeaders) match {
      case value if value.isEmpty => None
      case value => Some(value)
    }
  }

  def parseSignatureHeader(signatureHeader: String): Map[String, String] = {
    val regex = new Regex("""(\w+)\s*=\s*"([^"]*)"""", "key", "value")
    regex.findAllMatchIn(signatureHeader).map(m => m.group("key") -> m.group("value")).toMap
  }

  // Example Usage
  def main(args: Array[String]): Unit = {
    val requestBody = """"{
                        |  "access": {
                        |    "accounts": [
                        |      {
                        |        "iban": "RS35260005601001611379"
                        |      }
                        |    ],
                        |    "balances": [
                        |      {
                        |        "iban": "RS35260005601001611379"
                        |      }
                        |    ]
                        |  },
                        |  "recurringIndicator": true,
                        |  "validUntil": "2025-01-20T11:04:20Z",
                        |  "frequencyPerDay": 10,
                        |  "combinedServiceIndicator": false
                        |}"""".stripMargin
    val digest = calculateDigest(requestBody)

    val xRequestId = "12345678"
    val date = "Tue, 13 Feb 2024 10:00:00 GMT"
    val redirectUri = "www.redirect-uri.com"
    val headers = Map(
      RequestHeader.Digest -> s"SHA-256=$digest",
      RequestHeader.`X-Request-ID` -> xRequestId,
      RequestHeader.Date -> date,
      RequestHeader.`TPP-Redirect-URL` -> redirectUri,
    )

    val signingString = createSigningString(headers)

    // Load PEM files as strings
    val privateKeyPath = "/home/marko/Downloads/BerlinGroupSigning/private_key.pem"
    val certificatePath = "/home/marko/Downloads/BerlinGroupSigning/certificate.pem"

    val privateKeyPem = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
    val certificatePem = new String(Files.readAllBytes(Paths.get(certificatePath)))

    val signature = signString(signingString, privateKeyPem)
    val certificate = loadCertificate(certificatePem)

    println(s"1) Digest: SHA-256=$digest")
    println(s"2) ${RequestHeader.`X-Request-ID`}: $xRequestId")
    println(s"3) ${RequestHeader.Date}: $date")
    println(s"4) ${RequestHeader.`TPP-Redirect-URL`}: $redirectUri")
    val signatureHeaderValue =
      s"""keyId="SN=4000000010FC01D520258AB15EAF, CA=CN=D-eSystemTrustIB, O=IP STISC 1003600096694, C-MD", algorithm="rsa-sha256", headers="digest date x-request-id tpp-redirect-uri", signature="$signature"""".stripMargin
    println(s"5) Signature: $signatureHeaderValue")
    println(s"6) TPP-Signature-Certificate: $certificate")

    val isVerified = verifySignature(signingString, signature, certificatePem)
    println(s"Signature Verification: $isVerified")


    val parsedSignature = parseSignatureHeader(signatureHeaderValue)
    println(s"Parsed Signature Header: $parsedSignature")
  }
}
