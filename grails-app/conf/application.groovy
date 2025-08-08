// Added by Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'koolpump.user.KpUser'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'koolpump.user.UserRole'
grails.plugin.springsecurity.authority.className = 'koolpump.user.Role'
grails.plugin.springsecurity.userLookup.usernamePropertyName = 'email'

grails.plugin.springsecurity.controllerAnnotations.staticRules = [
    [pattern: '/',               access: ['permitAll']],
    [pattern: '/error',          access: ['permitAll']],
    [pattern: '/index',          access: ['permitAll']],
    [pattern: '/index.gsp',      access: ['permitAll']],
    [pattern: '/shutdown',       access: ['permitAll']],
    [pattern: '/assets/**',      access: ['permitAll']],
    [pattern: '/**/js/**',       access: ['permitAll']],
    [pattern: '/**/css/**',      access: ['permitAll']],
    [pattern: '/**/images/**',   access: ['permitAll']],
    [pattern: '/**/favicon.ico', access: ['permitAll']],
    [pattern: '/login/**',       access: ['permitAll']],
    [pattern: '/logout/**',      access: ['permitAll']],
    [pattern: '/registration/**', access: ['permitAll']],
    [pattern: '/passwordReset/**', access: ['permitAll']],
    [pattern: '/contact/**',     access: ['permitAll']],
    [pattern: '/auth/**',        access: ['permitAll']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
    [pattern: '/assets/**',      filters: 'none'],
    [pattern: '/**/js/**',       filters: 'none'],
    [pattern: '/**/css/**',      filters: 'none'],
    [pattern: '/**/images/**',   filters: 'none'],
    [pattern: '/**/favicon.ico', filters: 'none'],
    [pattern: '/**',             filters: 'JOINED_FILTERS']
]

// Password encoding
grails.plugin.springsecurity.password.algorithm = 'bcrypt'
grails.plugin.springsecurity.password.bcrypt.logrounds = 10

// Session configuration
grails.plugin.springsecurity.successHandler.alwaysUseDefault = false
grails.plugin.springsecurity.successHandler.defaultTargetUrl = '/dashboard'
grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.logout.afterLogoutUrl = '/'

// Remember me
grails.plugin.springsecurity.rememberMe.persistent = false
grails.plugin.springsecurity.rememberMe.persistentToken.domainClassName = 'koolpump.user.PersistentLogin'

// Additional security settings
grails.plugin.springsecurity.rejectIfNoRule = false
grails.plugin.springsecurity.fii.rejectPublicInvocations = false