package koolpump

class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        // Authentication routes
        "/login"(controller: "auth", action: "login")
        "/logout"(controller: "auth", action: "logout")
        "/authenticate"(controller: "auth", action: "authenticate")
        
        // Registration routes
        "/register"(controller: "registration", action: "register")
        "/register/save"(controller: "registration", action: "save")
        "/verify"(controller: "registration", action: "verify")
        
        // Password reset routes
        "/forgot-password"(controller: "passwordReset", action: "forgotPassword")
        "/password-reset/send"(controller: "passwordReset", action: "sendResetLink")
        "/password-reset/reset"(controller: "passwordReset", action: "resetPassword")
        "/password-reset/update"(controller: "passwordReset", action: "updatePassword")
        
        // Dashboard routes
        "/dashboard"(controller: "dashboard", action: "index")
        "/dashboard/profile"(controller: "dashboard", action: "profile")
        "/dashboard/change-password"(controller: "dashboard", action: "changePassword")
        
        // OAuth routes
        "/auth/google"(controller: "oauth", action: "googleLogin")
        "/auth/google/callback"(controller: "oauth", action: "googleCallback")
        "/auth/microsoft"(controller: "oauth", action: "microsoftLogin")
        "/auth/microsoft/callback"(controller: "oauth", action: "microsoftCallback")
        
        // Payment routes
        "/payment/choose-plan"(controller: "payment", action: "choosePlan")
        "/payment/checkout"(controller: "payment", action: "checkout")
        "/payment/success"(controller: "payment", action: "success")
        "/payment/cancel"(controller: "payment", action: "cancel")
        "/payment/manage-billing"(controller: "payment", action: "manageBilling")
        "/payment/update-method"(controller: "payment", action: "updatePaymentMethod")
        
        // Stripe webhook
        "/stripe/webhook"(controller: "stripeWebhook", action: "handleWebhook")
        
        // Admin routes
        "/admin"(controller: "admin", action: "index")
        "/admin/users"(controller: "admin", action: "users")
        "/admin/payments"(controller: "admin", action: "payments")
        "/admin/subscriptions"(controller: "admin", action: "subscriptions")
        "/admin/contact-messages"(controller: "admin", action: "contactMessages")
        
        // Contact routes
        "/contact"(controller: "contact", action: "index")
        "/contact/send"(controller: "contact", action: "send")
        "/contact/success"(controller: "contact", action: "success")

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')

    }
}
