<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Contact Us - KoolPump</title>
    <style>
        .contact-container {
            max-width: 800px;
            margin: 50px auto;
            padding: 30px;
            background: white;
            border-radius: 10px;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .contact-header {
            text-align: center;
            margin-bottom: 30px;
        }
        .contact-header h1 {
            color: #667eea;
        }
        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            margin-bottom: 20px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group.full-width {
            grid-column: 1 / -1;
        }
        .form-control {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 1rem;
        }
        .form-control:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
        }
        textarea.form-control {
            resize: vertical;
            min-height: 150px;
        }
        .btn-primary {
            padding: 12px 30px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 1.1rem;
            cursor: pointer;
            transition: transform 0.2s;
        }
        .btn-primary:hover {
            transform: translateY(-2px);
        }
        .error-message {
            color: #dc3545;
            font-size: 0.875rem;
            margin-top: 5px;
        }
        .info-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 30px 0;
        }
        .info-card {
            text-align: center;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 10px;
        }
        .info-card-icon {
            font-size: 2rem;
            margin-bottom: 10px;
            color: #667eea;
        }
        .newsletter-opt {
            display: flex;
            align-items: center;
            margin: 20px 0;
            padding: 15px;
            background: #f8f9fa;
            border-radius: 5px;
        }
        .newsletter-opt input[type="checkbox"] {
            margin-right: 10px;
        }
    </style>
</head>
<body>

<div class="contact-container">
    <div class="contact-header">
        <h1><g:message code="contact.title" default="Get in Touch"/></h1>
        <p><g:message code="contact.subtitle" default="We'd love to hear from you. Send us a message and we'll respond as soon as possible."/></p>
    </div>

    <g:if test="${flash.error}">
        <div class="alert alert-danger">${flash.error}</div>
    </g:if>

    <div class="info-cards">
        <div class="info-card">
            <div class="info-card-icon">📧</div>
            <h4>Email</h4>
            <p>support@koolpump.com</p>
        </div>
        <div class="info-card">
            <div class="info-card-icon">⏰</div>
            <h4>Response Time</h4>
            <p>24-48 hours</p>
        </div>
        <div class="info-card">
            <div class="info-card-icon">🌍</div>
            <h4>Coverage</h4>
            <p>All EU Countries</p>
        </div>
    </div>

    <g:form action="send" method="POST">
        <kpSec:csrfToken/>
        <div class="form-row">
            <div class="form-group">
                <label for="name"><g:message code="contact.name" default="Your Name"/> *</label>
                <input type="text" name="name" id="name" class="form-control" 
                       value="${contactMessage?.name}" required />
                <g:if test="${contactMessage?.errors?.hasFieldErrors('name')}">
                    <div class="error-message">${contactMessage.errors.getFieldError('name').defaultMessage}</div>
                </g:if>
            </div>
            
            <div class="form-group">
                <label for="email"><g:message code="contact.email" default="Email Address"/> *</label>
                <input type="email" name="email" id="email" class="form-control" 
                       value="${contactMessage?.email}" required />
                <g:if test="${contactMessage?.errors?.hasFieldErrors('email')}">
                    <div class="error-message">${contactMessage.errors.getFieldError('email').defaultMessage}</div>
                </g:if>
            </div>
        </div>

        <div class="form-row">
            <div class="form-group">
                <label for="category"><g:message code="contact.category" default="Category"/> *</label>
                <select name="category" id="category" class="form-control" required>
                    <option value="">Select a category...</option>
                    <option value="GENERAL" ${contactMessage?.category == 'GENERAL' ? 'selected' : ''}>
                        <g:message code="contact.category.general" default="General Inquiry"/>
                    </option>
                    <option value="SUPPORT" ${contactMessage?.category == 'SUPPORT' ? 'selected' : ''}>
                        <g:message code="contact.category.support" default="Technical Support"/>
                    </option>
                    <option value="SALES" ${contactMessage?.category == 'SALES' ? 'selected' : ''}>
                        <g:message code="contact.category.sales" default="Sales"/>
                    </option>
                    <option value="PARTNERSHIP" ${contactMessage?.category == 'PARTNERSHIP' ? 'selected' : ''}>
                        <g:message code="contact.category.partnership" default="Partnership"/>
                    </option>
                    <option value="BUG_REPORT" ${contactMessage?.category == 'BUG_REPORT' ? 'selected' : ''}>
                        <g:message code="contact.category.bug" default="Bug Report"/>
                    </option>
                </select>
                <g:if test="${contactMessage?.errors?.hasFieldErrors('category')}">
                    <div class="error-message">${contactMessage.errors.getFieldError('category').defaultMessage}</div>
                </g:if>
            </div>
            
            <div class="form-group">
                <label for="subject"><g:message code="contact.subject" default="Subject"/> *</label>
                <input type="text" name="subject" id="subject" class="form-control" 
                       value="${contactMessage?.subject}" required />
                <g:if test="${contactMessage?.errors?.hasFieldErrors('subject')}">
                    <div class="error-message">${contactMessage.errors.getFieldError('subject').defaultMessage}</div>
                </g:if>
            </div>
        </div>
        
        <div class="form-group full-width">
            <label for="message"><g:message code="contact.message" default="Message"/> *</label>
            <textarea name="message" id="message" class="form-control" 
                      required maxlength="5000">${contactMessage?.message}</textarea>
            <small class="text-muted">
                <span id="charCount">0</span> / 5000 characters
            </small>
            <g:if test="${contactMessage?.errors?.hasFieldErrors('message')}">
                <div class="error-message">${contactMessage.errors.getFieldError('message').defaultMessage}</div>
            </g:if>
        </div>

        <div class="newsletter-opt">
            <label>
                <input type="checkbox" name="subscribeNewsletter" value="true" 
                       ${session.user?.subscribeNewsletter ? 'checked' : ''} />
                <g:message code="contact.newsletter" default="Subscribe to our newsletter for heat pump news and updates"/>
            </label>
        </div>
        
        <div class="form-group">
            <button type="submit" class="btn-primary">
                <g:message code="contact.send" default="Send Message"/>
            </button>
        </div>
    </g:form>
</div>

<script>
    // Character counter
    const messageField = document.getElementById('message');
    const charCount = document.getElementById('charCount');
    
    function updateCharCount() {
        charCount.textContent = messageField.value.length;
    }
    
    messageField.addEventListener('input', updateCharCount);
    updateCharCount();
</script>

</body>
</html>