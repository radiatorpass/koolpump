# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Grails 6.2.3 web application using Gradle 7.6.4 build system, Java 11, and PostgreSQL database. The application follows standard Grails MVC architecture with GORM for data persistence.

The Url is www.koolpump.com

## Essential Commands

### Running the Application
```bash
#Init env

set -a && source /home/koolpump/koolpump/.env && set +a

# Start development server
./gradlew bootRun

# Clean and build
./gradlew clean build

# Run with specific environment
./gradlew -Dgrails.env=production bootRun
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run integration tests with browser
./gradlew -Dgeb.env=chrome integrationTest
./gradlew -Dgeb.env=chromeHeadless integrationTest
./gradlew -Dgeb.env=firefox integrationTest

# Run all checks (tests + code quality)
./gradlew check

# Run specific test class
./gradlew test --tests "koolpump.*SpecificSpec"
```

### Database Setup
```bash
# Load environment variables
source loadsources.sh

# Required databases:
# - devDb (development)
# - testDb (testing)
# - postgres (production)
```

## Architecture

### Core Stack
- **Framework**: Grails 6.2.3 with Spring Boot
- **ORM**: GORM with Hibernate 5
- **Database**: PostgreSQL with Liquibase migrations
- **Testing**: Spock (unit), Geb (functional), Testcontainers (integration)
- **Build**: Gradle with asset pipeline
- **Scheduling**: Quartz plugin
- **Caching**: Grails Cache plugin

### Project Structure
- `grails-app/controllers/` - Request handlers following Grails conventions
- `grails-app/domain/` - GORM domain classes (data models)
- `grails-app/services/` - Business logic layer
- `grails-app/views/` - GSP templates and JSON views
- `grails-app/conf/` - Configuration files (application.yml, logback)
- `src/main/groovy/` - Non-Grails Groovy classes
- `src/test/groovy/` - Unit tests using Spock
- `src/integration-test/groovy/` - Integration tests with Geb

### Database Configuration
Environment variables required (.env file):
- `DB_USER` - PostgreSQL username
- `DB_PASSWORD` - PostgreSQL password

Connection URLs:
- Development: `jdbc:postgresql://localhost:5432/devDb`
- Test: `jdbc:postgresql://localhost:5432/testDb`
- Production: `jdbc:postgresql://localhost:5432/postgres`


### Grails init commands

curl --location --request GET 'https://latest.grails.org/create/web/koolpump.koolpump?gorm=HIBERNATE&servlet=TOMCAT&test=SPOCK&javaVersion=JDK_11&features=github-workflow-java-ci&features=cache&features=database-migration&features=postgres&features=spring-boot-devtools&features=logbackGroovy&features=grails-web-console&features=grails-quartz&features=hibernate-validator&features=views-json' --output koolpump.zip


### Key Plugins and Dependencies
- **grails-plugin-postgresql** - PostgreSQL JDBC driver
- **grails-plugin-quartz** - Job scheduling
- **grails-plugin-cache** - Caching support
- **grails-plugin-geb** - Functional testing
- **testcontainers** - Isolated database testing
- **liquibase** - Database migrations
- **spring-boot-devtools** - Hot reload in development

### Testing Strategy
1. **Unit Tests**: Located in `src/test/groovy/`, use Spock framework
2. **Integration Tests**: Located in `src/integration-test/groovy/`, use Geb for browser automation
3. **Database Tests**: Use Testcontainers for PostgreSQL isolation
4. **Browser Support**: Chrome, Firefox, Safari drivers configured

### CI/CD
GitHub Actions workflow (`.github/workflows/gradle.yml`):
- Runs on push and pull requests
- Uses Java 11 (Liberica distribution)
- PR: Runs `./gradlew check`
- Push: Runs `./gradlew build`

## Development Notes

### Creating New Components
- Controllers: Place in `grails-app/controllers/koolpump/`
- Domain classes: Place in `grails-app/domain/koolpump/`
- Services: Place in `grails-app/services/koolpump/`
- Use Grails conventions for naming (e.g., `BookController`, `BookService`)

### URL Mappings
Defined in `grails-app/controllers/koolpump/UrlMappings.groovy`
- Follow RESTful conventions
- Custom error pages configured for 404 and 500

### Logging
Configured via `grails-app/conf/logback-config.groovy`
- Environment-specific levels
- Supports `LOG_LEVEL` and `LOG_NAME` environment variables

### Asset Pipeline
- JavaScript: `grails-app/assets/javascripts/`
- CSS: `grails-app/assets/stylesheets/`
- Images: `grails-app/assets/images/`
- Bootstrap and jQuery pre-configured