import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
}

val catalogs = extensions.getByType<VersionCatalogsExtension>()
val libsCatalog = catalogs.named("libs")
val ktlintVersion = libsCatalog.findVersion("ktlint").get().requiredVersion
val detektVersion = libsCatalog.findVersion("detekt").get().requiredVersion

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<SpotlessExtension>("spotless") {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint(ktlintVersion).editorConfigOverride(
                mapOf(
                    "indent_size" to "4",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_max-line-length" to "disabled",
                    "ktlint_standard_function-naming" to "disabled"
                )
            )
        }

        kotlinGradle {
            target("**/*.kts")
            targetExclude("**/build/**/*.kts")
            ktlint(ktlintVersion)
        }

        format("misc") {
            target("**/*.md", "**/.gitignore")
            targetExclude("**/build/**")
            trimTrailingWhitespace()
            endWithNewline()
        }

        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**")
            eclipseWtp(EclipseWtpFormatterStep.XML)
        }
    }

    extensions.configure<DetektExtension>("detekt") {
        toolVersion = detektVersion
        config.setFrom(files("${rootDir}/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        autoCorrect = false
        baseline = file("${rootDir}/config/detekt/baseline.xml")
    }

    tasks.withType(Detekt::class.java).configureEach {
        jvmTarget = "17"
        setSource(files("src"))
        include("**/*.kt")
        exclude("**/build/**")
        reports {
            html.required.set(true)
            xml.required.set(true)
            txt.required.set(false)
            sarif.required.set(false)
        }
    }

    tasks.named("check").configure {
        dependsOn("spotlessCheck", "detekt")
    }
}
