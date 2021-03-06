package tech.kzen.launcher.server.service

import com.google.common.io.ByteStreams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import java.net.URI
import java.rmi.server.RMISocketFactory.getSocketFactory
import java.security.cert.X509Certificate
import javax.annotation.PostConstruct
import javax.net.ssl.*


@Component
class DownloadService {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private val logger = LoggerFactory.getLogger(DownloadService::class.java)!!
    }


    //-----------------------------------------------------------------------------------------------------------------
    // TODO: implement proper certificate management
    @PostConstruct
    fun trustBadCertificate() {
        // https://stackoverflow.com/a/24501156

        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        })

        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

        val allHostsValid = HostnameVerifier { _, _ -> true }

        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun download(location: URI): ByteArray {
        logger.info("downloading: {}", location)

        val bytes = location
                .toURL()
                .openStream()
                .use { ByteStreams.toByteArray(it) }

        logger.info("download complete: {}", bytes.size)

        return bytes
    }
}