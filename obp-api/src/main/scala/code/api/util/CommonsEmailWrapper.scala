package code.api.util

import code.util.Helper.MdcLoggable
import net.liftweb.common.{Box, Empty, Full}
import org.apache.commons.mail._

import java.net.URL

/**
 * Apache Commons Email Wrapper for OBP-API
 * This wrapper provides a simple interface to send emails using Apache Commons Email
 * instead of Lift Web's Mailer
 */
object CommonsEmailWrapper extends MdcLoggable {

  /**
   * Email configuration case class
   */
  case class EmailConfig(
    smtpHost: String,
    smtpPort: Int,
    username: String,
    password: String,
    useTLS: Boolean = true,
    useSSL: Boolean = false,
    debug: Boolean = false,
    tlsProtocols: String = "TLSv1.2"  // TLS protocols to use
  )

  /**
   * Email content case class
   */
  case class EmailContent(
    from: String,
    to: List[String],
    cc: List[String] = List.empty,
    bcc: List[String] = List.empty,
    subject: String,
    textContent: Option[String] = None,
    htmlContent: Option[String] = None,
    attachments: List[EmailAttachment] = List.empty
  )

  /**
   * Get default email configuration from OBP-API properties
   */
  def getDefaultEmailConfig(): EmailConfig = {
    EmailConfig(
      smtpHost = APIUtil.getPropsValue("mail.smtp.host", "localhost"),
      smtpPort = APIUtil.getPropsValue("mail.smtp.port", "1025").toInt,
      username = APIUtil.getPropsValue("mail.smtp.user", ""),
      password = APIUtil.getPropsValue("mail.smtp.password", ""),
      useTLS = APIUtil.getPropsValue("mail.smtp.starttls.enable", "false").toBoolean,
      useSSL = APIUtil.getPropsValue("mail.smtp.ssl.enable", "false").toBoolean,
      debug = APIUtil.getPropsValue("mail.debug", "false").toBoolean,
      tlsProtocols = APIUtil.getPropsValue("mail.smtp.ssl.protocols", "TLSv1.2")
    )
  }

  /**
   * Send simple text email with default configuration
   */
  def sendTextEmail(content: EmailContent): Box[String] = {
    sendTextEmail(getDefaultEmailConfig(), content)
  }

  /**
   * Send HTML email with default configuration
   */
  def sendHtmlEmail(content: EmailContent): Box[String] = {
    sendHtmlEmail(getDefaultEmailConfig(), content)
  }

  /**
   * Send email with attachments using default configuration
   */
  def sendEmailWithAttachments(content: EmailContent): Box[String] = {
    sendEmailWithAttachments(getDefaultEmailConfig(), content)
  }

  /**
   * Send simple text email
   */
  def sendTextEmail(config: EmailConfig, content: EmailContent): Box[String] = {
    try {
      logger.info(s"Sending text email from ${content.from} to ${content.to.mkString(", ")}")
      
      val email = new SimpleEmail()
      configureEmail(email, config, content)
      
      // Set text content
      content.textContent match {
        case Some(text) => email.setMsg(text)
        case None => email.setMsg("")
      }
      
      val messageId = email.send()
      logger.info(s"Email sent successfully with Message-ID: $messageId")
      Full(messageId)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to send text email: ${e.getMessage}", e)
        Empty
    }
  }

  /**
   * Send HTML email
   */
  def sendHtmlEmail(config: EmailConfig, content: EmailContent): Box[String] = {
    try {
      logger.info(s"Sending HTML email from ${content.from} to ${content.to.mkString(", ")}")
      
      val email = new HtmlEmail()
      configureEmail(email, config, content)
      
      // Set HTML content
      content.htmlContent match {
        case Some(html) => email.setHtmlMsg(html)
        case None => email.setHtmlMsg("<html><body>No content</body></html>")
      }
      
      // Set text content as fallback
      content.textContent.foreach(email.setTextMsg)
      
      val messageId = email.send()
      logger.info(s"HTML email sent successfully with Message-ID: $messageId")
      Full(messageId)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to send HTML email: ${e.getMessage}", e)
        Empty
    }
  }

  /**
   * Send email with attachments
   */
  def sendEmailWithAttachments(config: EmailConfig, content: EmailContent): Box[String] = {
    try {
      logger.info(s"Sending email with attachments from ${content.from} to ${content.to.mkString(", ")}")
      
      val email = new MultiPartEmail()
      configureEmail(email, config, content)
      
      // Set text content
      content.textContent.foreach(email.setMsg)
      
      // Add attachments
      content.attachments.foreach(email.attach)
      
      val messageId = email.send()
      logger.info(s"Email with attachments sent successfully with Message-ID: $messageId")
      Full(messageId)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to send email with attachments: ${e.getMessage}", e)
        Empty
    }
  }

  /**
   * Configure email with common settings
   */
  private def configureEmail(email: Email, config: EmailConfig, content: EmailContent): Unit = {
    // SMTP Configuration
    email.setHostName(config.smtpHost)
    email.setSmtpPort(config.smtpPort)
    email.setAuthenticator(new DefaultAuthenticator(config.username, config.password))
    email.setSSLOnConnect(config.useSSL)
    email.setStartTLSEnabled(config.useTLS)
    email.setDebug(config.debug)
    email.getMailSession.getProperties.setProperty("mail.smtp.ssl.protocols", config.tlsProtocols)
    
    // Set charset
    email.setCharset("UTF-8")
    
    // Set sender
    email.setFrom(content.from)
    
    // Set recipients
    content.to.foreach(email.addTo)
    content.cc.foreach(email.addCc)
    content.bcc.foreach(email.addBcc)
    
    // Set subject
    email.setSubject(content.subject)
  }

  /**
   * Create email attachment from file
   */
  def createFileAttachment(filePath: String, name: Option[String] = None): EmailAttachment = {
    val attachment = new EmailAttachment()
    attachment.setPath(filePath)
    attachment.setDisposition(EmailAttachment.ATTACHMENT)
    name.foreach(attachment.setName)
    attachment
  }

  /**
   * Create email attachment from URL
   */
  def createUrlAttachment(url: String, name: String): EmailAttachment = {
    val attachment = new EmailAttachment()
    attachment.setURL(new URL(url))
    attachment.setDisposition(EmailAttachment.ATTACHMENT)
    attachment.setName(name)
    attachment
  }

} 