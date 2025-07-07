package code.api.util.migration

import code.api.util.APIUtil
import code.api.util.migration.Migration.{DbFunction, saveLog}
import code.views.MapperViews
import code.views.system.{ViewDefinition, ViewPermission}

object MigrationOfViewPermissions {
  def populate(name: String): Boolean = {
    DbFunction.tableExists(ViewDefinition) && DbFunction.tableExists(ViewPermission)match {
      case true =>
        val startDate = System.currentTimeMillis()
        val commitId: String = APIUtil.gitCommit
        
        val allViewDefinitions = ViewDefinition.findAll()
        allViewDefinitions.map(v => MapperViews.migrateViewPermissions(v))
        
        val isSuccessful = true
        val endDate = System.currentTimeMillis()

        val comment: String = s"""migrate all permissions from ViewDefinition (${allViewDefinitions.length} rows) to ViewPermission .""".stripMargin
        saveLog(name, commitId, isSuccessful, startDate, endDate, comment)
        isSuccessful
        
      case false =>
        val startDate = System.currentTimeMillis()
        val commitId: String = APIUtil.gitCommit
        val isSuccessful = false
        val endDate = System.currentTimeMillis()
        val comment: String =
          s"""ViewDefinition or ViewPermission does not exist!""".stripMargin
        saveLog(name, commitId, isSuccessful, startDate, endDate, comment)
        isSuccessful
    }
  }
}
