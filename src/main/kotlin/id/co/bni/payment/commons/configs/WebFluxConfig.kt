package id.co.bni.payment.commons.configs

import id.co.bni.payment.applications.resolvers.SubjectArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
class WebFluxConfig(
    private val subjectArgumentResolver: SubjectArgumentResolver
) : WebFluxConfigurer {
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(subjectArgumentResolver)
    }
}