package db.migration

import java.sql.Connection

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import scalikejdbc.{DB, DBSession, _}

class V24__UseTwoLetteredLanguageCodes extends JdbcMigration with LazyLogging {
  override def migrate(connection: Connection): Unit = {

    val db = DB(connection)
    db.autoClose(false)

    logger.info("Starting V24__UseTwoLetteredLanguageCodes DB Migration")

    db.withinTx { implicit session =>
      allTranslations.foreach(update)
      allInTranslations.map(modifyProjectId).foreach(update)
    }

    logger.info("Done V24__UseTwoLetteredLanguageCodes DB Migration")
  }

  def allTranslations(implicit session: DBSession): List[V24_Translation] = {
    sql"select id, language from translation".map(rs =>
      V24_Translation(id = rs.long("id"), language = LanguageTag(rs.string("language")))
    ).list().apply()
  }

  def update(translation: V24_Translation)(implicit session: DBSession): Int = {
    sql"update translation set language = ${translation.language.toString} where id = ${translation.id}"
      .update().apply
  }

  def allInTranslations(implicit session: DBSession): List[V24_InTranslation] = {
    sql"select id, from_language, to_language, crowdin_project_id from in_translation".map(rs =>
      V24_InTranslation(
        id = rs.long("id"),
        fromLanguage = LanguageTag(rs.string("from_language")),
        toLanguage = LanguageTag(rs.string("to_language")),
        crowdinProjectId = rs.string("crowdin_project_id"))
    ).list().apply()
  }

  def modifyProjectId(inTranslation: V24_InTranslation): V24_InTranslation = {
    inTranslation.crowdinProjectId.split('-').toList match {
      case project :: env :: lang :: Nil => inTranslation.copy(crowdinProjectId = s"$project-$env-${LanguageTag(lang).toString}")
      case _ => throw new IllegalArgumentException
    }
  }

  def update(it: V24_InTranslation)(implicit session: DBSession): Int = {
    sql"update in_translation set from_language = ${it.fromLanguage.toString}, to_language = ${it.toLanguage.toString}, crowdin_project_id = ${it.crowdinProjectId} where id = ${it.id}"
      .update().apply
  }

}

case class V24_Translation(id: Long, language: LanguageTag)

case class V24_InTranslation(id: Long, fromLanguage: LanguageTag, toLanguage: LanguageTag, crowdinProjectId: String)
