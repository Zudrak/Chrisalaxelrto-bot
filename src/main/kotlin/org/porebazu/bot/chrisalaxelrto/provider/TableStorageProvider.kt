package org.porebazu.bot.chrisalaxelrto.provider
import com.azure.core.http.policy.HttpLogDetailLevel
import com.azure.core.http.policy.HttpLogOptions
import com.azure.data.tables.TableClient
import com.azure.data.tables.TableClientBuilder
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertySource
import org.springframework.stereotype.Service

@Service
class TableStorageProvider(environment: Environment, identityProvider: IdentityProvider) : PropertySource<Any>("TableStorage") {
    private val accountName: String = environment.getProperty("table-storage-account-name") ?: ""
    private val tableServiceUrl = "https://$accountName.table.core.windows.net"
    private val credential = identityProvider.getCredential()
    private lateinit var tableClient: TableClient
    init {
        if(accountName.isEmpty()) {
            throw IllegalArgumentException("Table storage account name is not set in the environment.")
        }

        tableClient = TableClientBuilder()
            .endpoint(tableServiceUrl)
            .credential(credential)
            .tableName("ChrisalaxelrtoConfig")
            .httpLogOptions(HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
            .buildClient()
    }
    override fun getProperty(name: String): Any? {
        if(name.startsWith("spring.")){
            return null
        }
        val entity = tableClient.getEntity("724848650810818601", "724848650810818601")
        return entity?.getProperty(name)
    }
}