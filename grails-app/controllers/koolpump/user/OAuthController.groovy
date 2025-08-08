package koolpump.user

import grails.converters.JSON
import grails.core.GrailsApplication
import java.net.URLEncoder

class OAuthController {

    UserService userService
    GrailsApplication grailsApplication

    def googleLogin() {
        def clientId = System.getenv('GOOGLE_CLIENT_ID') ?: grailsApplication.config.oauth.google.clientId
        def redirectUri = URLEncoder.encode("${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}/auth/google/callback", "UTF-8")
        def scope = URLEncoder.encode("openid email profile", "UTF-8")
        
        def authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=${clientId}" +
            "&redirect_uri=${redirectUri}" +
            "&response_type=code" +
            "&scope=${scope}" +
            "&access_type=offline" +
            "&prompt=consent"
        
        redirect(url: authUrl)
    }

    def googleCallback() {
        def code = params.code
        def error = params.error
        
        if (error) {
            flash.error = "Google authentication cancelled"
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        if (!code) {
            flash.error = "Invalid authentication response from Google"
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        try {
            def tokenData = exchangeGoogleCode(code)
            def userInfo = getGoogleUserInfo(tokenData.access_token)
            
            def user = userService.findOrCreateOAuthUser(
                userInfo.email,
                userInfo.given_name ?: userInfo.name?.split(' ')[0] ?: 'User',
                userInfo.family_name ?: userInfo.name?.split(' ')[-1] ?: 'User',
                'GOOGLE',
                userInfo.sub
            )
            
            if (user) {
                session.user = user
                session.userId = user.id
                userService.logActivity(user, 'OAUTH_LOGIN_GOOGLE', request.getRemoteAddr())
                
                redirect(controller: 'dashboard', action: 'index')
            } else {
                flash.error = "Failed to create user account"
                redirect(controller: 'auth', action: 'login')
            }
        } catch (Exception e) {
            log.error "Google OAuth error: ${e.message}", e
            flash.error = "Authentication failed. Please try again."
            redirect(controller: 'auth', action: 'login')
        }
    }

    def microsoftLogin() {
        def clientId = System.getenv('MICROSOFT_CLIENT_ID') ?: grailsApplication.config.oauth.microsoft.clientId
        def redirectUri = URLEncoder.encode("${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}/auth/microsoft/callback", "UTF-8")
        def scope = URLEncoder.encode("openid email profile", "UTF-8")
        
        def authUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?" +
            "client_id=${clientId}" +
            "&redirect_uri=${redirectUri}" +
            "&response_type=code" +
            "&scope=${scope}" +
            "&response_mode=query"
        
        redirect(url: authUrl)
    }

    def microsoftCallback() {
        def code = params.code
        def error = params.error
        
        if (error) {
            flash.error = "Microsoft authentication cancelled"
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        if (!code) {
            flash.error = "Invalid authentication response from Microsoft"
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        try {
            def tokenData = exchangeMicrosoftCode(code)
            def userInfo = getMicrosoftUserInfo(tokenData.access_token)
            
            def user = userService.findOrCreateOAuthUser(
                userInfo.mail ?: userInfo.userPrincipalName,
                userInfo.givenName ?: 'User',
                userInfo.surname ?: 'User',
                'MICROSOFT',
                userInfo.id
            )
            
            if (user) {
                session.user = user
                session.userId = user.id
                userService.logActivity(user, 'OAUTH_LOGIN_MICROSOFT', request.getRemoteAddr())
                
                redirect(controller: 'dashboard', action: 'index')
            } else {
                flash.error = "Failed to create user account"
                redirect(controller: 'auth', action: 'login')
            }
        } catch (Exception e) {
            log.error "Microsoft OAuth error: ${e.message}", e
            flash.error = "Authentication failed. Please try again."
            redirect(controller: 'auth', action: 'login')
        }
    }

    private Map exchangeGoogleCode(String code) {
        def clientId = System.getenv('GOOGLE_CLIENT_ID') ?: grailsApplication.config.oauth.google.clientId
        def clientSecret = System.getenv('GOOGLE_CLIENT_SECRET') ?: grailsApplication.config.oauth.google.clientSecret
        def redirectUri = "${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}/auth/google/callback"
        
        def url = new URL("https://oauth2.googleapis.com/token")
        def connection = url.openConnection() as HttpURLConnection
        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        
        def params = "code=${code}" +
            "&client_id=${clientId}" +
            "&client_secret=${clientSecret}" +
            "&redirect_uri=${URLEncoder.encode(redirectUri, 'UTF-8')}" +
            "&grant_type=authorization_code"
        
        connection.outputStream.withWriter { it.write(params) }
        
        def response = connection.inputStream.text
        return JSON.parse(response)
    }

    private Map getGoogleUserInfo(String accessToken) {
        def url = new URL("https://www.googleapis.com/oauth2/v2/userinfo")
        def connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer ${accessToken}")
        
        def response = connection.inputStream.text
        return JSON.parse(response)
    }

    private Map exchangeMicrosoftCode(String code) {
        def clientId = System.getenv('MICROSOFT_CLIENT_ID') ?: grailsApplication.config.oauth.microsoft.clientId
        def clientSecret = System.getenv('MICROSOFT_CLIENT_SECRET') ?: grailsApplication.config.oauth.microsoft.clientSecret
        def redirectUri = "${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}/auth/microsoft/callback"
        
        def url = new URL("https://login.microsoftonline.com/common/oauth2/v2.0/token")
        def connection = url.openConnection() as HttpURLConnection
        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        
        def params = "code=${code}" +
            "&client_id=${clientId}" +
            "&client_secret=${clientSecret}" +
            "&redirect_uri=${URLEncoder.encode(redirectUri, 'UTF-8')}" +
            "&grant_type=authorization_code"
        
        connection.outputStream.withWriter { it.write(params) }
        
        def response = connection.inputStream.text
        return JSON.parse(response)
    }

    private Map getMicrosoftUserInfo(String accessToken) {
        def url = new URL("https://graph.microsoft.com/v1.0/me")
        def connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer ${accessToken}")
        
        def response = connection.inputStream.text
        return JSON.parse(response)
    }
}