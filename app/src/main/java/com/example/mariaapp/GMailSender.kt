package com.example.mariaapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object GMailSender {

    // ⚠️ 請填入你的 Gmail 帳號
    private const val SENDER_EMAIL = "shine941011@gmail.com"

    // ⚠️ 請填入剛剛申請的 16 位數「應用程式密碼」 (不是登入密碼！)
    private const val APP_PASSWORD = "ogmgjhgyafdomgcn"

    suspend fun sendEmail(recipientEmail: String, subject: String, body: String) {
        withContext(Dispatchers.IO) { // 必須在背景執行網路操作
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.port", "465")
                }

                val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)
                println("郵件發送成功！")
            } catch (e: Exception) {
                e.printStackTrace()
                throw e // 丟出錯誤讓外面知道
            }
        }
    }
}