<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="site.title" default="KoolPump - European Heat Pump Database"/></title>
    <style>
        .hero-section {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 80px 0;
            text-align: center;
        }
        .hero-title {
            font-size: 3.5rem;
            font-weight: 700;
            margin-bottom: 20px;
        }
        .hero-subtitle {
            font-size: 1.5rem;
            opacity: 0.95;
            margin-bottom: 40px;
        }
        .search-container {
            max-width: 600px;
            margin: 0 auto;
        }
        .search-box {
            padding: 15px 25px;
            font-size: 1.1rem;
            border: none;
            border-radius: 50px;
            width: 100%;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        .feature-card {
            background: white;
            border-radius: 10px;
            padding: 30px;
            margin: 20px 0;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
            transition: transform 0.3s;
        }
        .feature-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 30px rgba(0,0,0,0.15);
        }
        .feature-icon {
            font-size: 3rem;
            margin-bottom: 20px;
            color: #667eea;
        }
        .stats-section {
            background: #f8f9fa;
            padding: 60px 0;
        }
        .stat-box {
            text-align: center;
            padding: 20px;
        }
        .stat-number {
            font-size: 3rem;
            font-weight: bold;
            color: #667eea;
        }
        .stat-label {
            font-size: 1.2rem;
            color: #6c757d;
        }
        .language-selector {
            position: absolute;
            top: 20px;
            right: 20px;
            z-index: 1000;
        }
        .language-selector select {
            padding: 8px 15px;
            border-radius: 5px;
            border: 1px solid #ddd;
            background: white;
            cursor: pointer;
        }
        .cta-button {
            display: inline-block;
            padding: 15px 40px;
            background: white;
            color: #667eea;
            text-decoration: none;
            border-radius: 50px;
            font-weight: bold;
            font-size: 1.1rem;
            transition: all 0.3s;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .cta-button:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 30px rgba(0,0,0,0.15);
            text-decoration: none;
            color: #764ba2;
        }
        .partner-logos {
            display: flex;
            justify-content: center;
            align-items: center;
            flex-wrap: wrap;
            gap: 40px;
            padding: 40px 0;
        }
        .partner-logo {
            opacity: 0.7;
            transition: opacity 0.3s;
        }
        .partner-logo:hover {
            opacity: 1;
        }
    </style>
</head>
<body>

<div class="language-selector">
    <select id="languageSelect" onchange="changeLanguage(this.value)">
        <option value="en" ${params.lang == 'en' || !params.lang ? 'selected' : ''}><g:message code="lang.english" default="English"/></option>
        <option value="de" ${params.lang == 'de' ? 'selected' : ''}><g:message code="lang.german" default="Deutsch"/></option>
        <option value="fr" ${params.lang == 'fr' ? 'selected' : ''}><g:message code="lang.french" default="Français"/></option>
        <option value="it" ${params.lang == 'it' ? 'selected' : ''}><g:message code="lang.italian" default="Italiano"/></option>
        <option value="es" ${params.lang == 'es' ? 'selected' : ''}><g:message code="lang.spanish" default="Español"/></option>
        <option value="nl" ${params.lang == 'nl' ? 'selected' : ''}><g:message code="lang.dutch" default="Nederlands"/></option>
        <option value="pl" ${params.lang == 'pl' ? 'selected' : ''}><g:message code="lang.polish" default="Polski"/></option>
        <option value="sv" ${params.lang == 'sv' ? 'selected' : ''}><g:message code="lang.swedish" default="Svenska"/></option>
    </select>
</div>

<div class="hero-section">
    <div class="container">
        <h1 class="hero-title"><g:message code="hero.title" default="European Heat Pump Database"/></h1>
        <p class="hero-subtitle"><g:message code="hero.subtitle" default="Your comprehensive resource for heat pump specifications, efficiency ratings, and certified installers across Europe"/></p>
        
        <div class="search-container">
            <input type="text" class="search-box" placeholder="${message(code: 'search.placeholder', default: 'Search heat pumps by brand, model, or specifications...')}" />
        </div>
        
        <div style="margin-top: 40px;">
            <a href="#features" class="cta-button"><g:message code="cta.explore" default="Explore Database"/></a>
        </div>
    </div>
</div>

<div class="stats-section">
    <div class="container">
        <div class="row">
            <div class="col-md-3">
                <div class="stat-box">
                    <div class="stat-number">2,500+</div>
                    <div class="stat-label"><g:message code="stats.models" default="Heat Pump Models"/></div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-box">
                    <div class="stat-number">150+</div>
                    <div class="stat-label"><g:message code="stats.manufacturers" default="Manufacturers"/></div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-box">
                    <div class="stat-number">28</div>
                    <div class="stat-label"><g:message code="stats.countries" default="EU Countries"/></div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-box">
                    <div class="stat-number">10,000+</div>
                    <div class="stat-label"><g:message code="stats.installers" default="Certified Installers"/></div>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="features" class="container" style="padding: 60px 0;">
    <h2 class="text-center mb-5"><g:message code="features.title" default="Comprehensive Heat Pump Intelligence"/></h2>
    
    <div class="row">
        <div class="col-md-4">
            <div class="feature-card">
                <div class="feature-icon">📊</div>
                <h3><g:message code="feature.performance.title" default="Performance Data"/></h3>
                <p><g:message code="feature.performance.desc" default="Access detailed SCOP, COP, and efficiency ratings for all climate zones. Compare seasonal performance across different operating conditions."/></p>
            </div>
        </div>
        
        <div class="col-md-4">
            <div class="feature-card">
                <div class="feature-icon">🏆</div>
                <h3><g:message code="feature.certification.title" default="EU Certifications"/></h3>
                <p><g:message code="feature.certification.desc" default="Verified EU energy labels, Keymark certifications, and compliance with Ecodesign regulations. Updated with latest ErP directives."/></p>
            </div>
        </div>
        
        <div class="col-md-4">
            <div class="feature-card">
                <div class="feature-icon">💰</div>
                <h3><g:message code="feature.incentives.title" default="Subsidy Calculator"/></h3>
                <p><g:message code="feature.incentives.desc" default="Calculate available subsidies and incentives by country and region. Stay updated with current grant schemes and tax benefits."/></p>
            </div>
        </div>
    </div>
    
    <div class="row">
        <div class="col-md-4">
            <div class="feature-card">
                <div class="feature-icon">🔧</div>
                <h3><g:message code="feature.technical.title" default="Technical Specs"/></h3>
                <p><g:message code="feature.technical.desc" default="Complete technical documentation including dimensions, refrigerants, sound levels, and installation requirements."/></p>
            </div>
        </div>
        
        <div class="col-md-4">
            <div class="feature-card">
                <div class="feature-icon">🌍</div>
                <h3><g:message code="feature.environmental.title" default="Environmental Impact"/></h3>
                <p><g:message code="feature.environmental.desc" default="CO2 savings calculator, GWP ratings, and environmental product declarations for sustainable heating choices."/></p>
            </div>
        </div>
        
        <div class="col-md-4">
            <div class="feature-card">
                <div class="feature-icon">👥</div>
                <h3><g:message code="feature.installer.title" default="Installer Network"/></h3>
                <p><g:message code="feature.installer.desc" default="Connect with certified installers in your area. Access reviews, certifications, and request quotes directly."/></p>
            </div>
        </div>
    </div>
</div>

<div style="background: #f8f9fa; padding: 60px 0;">
    <div class="container">
        <h2 class="text-center mb-5"><g:message code="partners.title" default="Trusted by Industry Leaders"/></h2>
        <div class="partner-logos">
            <div class="partner-logo">
                <img src="${resource(dir: 'images', file: 'partner-placeholder.png')}" alt="Partner" style="height: 60px;" />
            </div>
            <div class="partner-logo">
                <img src="${resource(dir: 'images', file: 'partner-placeholder.png')}" alt="Partner" style="height: 60px;" />
            </div>
            <div class="partner-logo">
                <img src="${resource(dir: 'images', file: 'partner-placeholder.png')}" alt="Partner" style="height: 60px;" />
            </div>
            <div class="partner-logo">
                <img src="${resource(dir: 'images', file: 'partner-placeholder.png')}" alt="Partner" style="height: 60px;" />
            </div>
        </div>
    </div>
</div>

<div class="container" style="padding: 60px 0;">
    <div class="text-center">
        <h2><g:message code="cta.final.title" default="Ready to Find Your Perfect Heat Pump?"/></h2>
        <p class="lead mt-3 mb-4"><g:message code="cta.final.subtitle" default="Join thousands of homeowners and professionals using KoolPump to make informed decisions"/></p>
        <a href="#" class="cta-button" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;">
            <g:message code="cta.getstarted" default="Get Started Free"/>
        </a>
    </div>
</div>

<script>
function changeLanguage(lang) {
    window.location.href = '/?lang=' + lang;
}
</script>

</body>
</html>