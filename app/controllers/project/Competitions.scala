package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.Requests.AuthRequest
import db.ModelService
import form.OreForms
import models.competition.Competition
import ore.OreConfig
import ore.permission.EditCompetitions
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.MessagesApi
import util.StringUtils
import views.{html => views}

/**
  * Handles competition based actions.
  */
class Competitions @Inject()(implicit override val messagesApi: MessagesApi,
                             override val config: OreConfig,
                             override val service: ModelService,
                             override val sso: SingleSignOnConsumer,
                             forms: OreForms)
                             extends BaseController {

  private val self = routes.Competitions

  private def EditCompetitionsAction = Authenticated andThen PermissionAction[AuthRequest](EditCompetitions)

  /**
    * Shows the competition administrative panel.
    *
    * @return Competition manager
    */
  def showManager() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.manage())
  }

  /**
    * Shows the competition creator.
    *
    * @return Competition creator
    */
  def showCreator() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.create())
  }

  /**
    * Creates a new competition.
    *
    * @return Redirect to manager or creator with errors.
    */
  def create() = EditCompetitionsAction { implicit request =>
    this.forms.CompetitionCreate.bindFromRequest().fold(
      hasErrors =>
        FormError(self.showCreator(), hasErrors),
      formData => {
        if (!formData.checkDates())
          Redirect(self.showCreator()).withError("error.dates.competition")
        else if (this.competitions.exists(StringUtils.equalsIgnoreCase(_.name, formData.name)))
          Redirect(self.showCreator()).withError("error.unique.competition.name")
        else {
          this.competitions.add(new Competition(request.user, formData))
          Redirect(self.showManager())
        }
      }
    )
  }

  /**
    * Saves the competition with the specified ID.
    *
    * @param id Competition ID
    * @return   Redirect to manager
    */
  def save(id: Int) = (EditCompetitionsAction andThen AuthedCompetitionAction(id)) { implicit request =>
    println(request.body.asFormUrlEncoded)
    this.forms.CompetitionSave.bindFromRequest().fold(
      hasErrors =>
        FormError(self.showManager(), hasErrors),
      formData => {
        if (!formData.checkDates())
          Redirect(self.showManager()).withError("error.dates.competition")
        else {
          request.competition.save(formData)
          Redirect(self.showManager()).withSuccess("success.saved.competition")
        }
      }
    )
  }

  /**
    * Deletes the competition with the specified ID.
    *
    * @param id Competition ID
    * @return   Redirect to manager
    */
  def delete(id: Int) = (EditCompetitionsAction andThen AuthedCompetitionAction(id)) { implicit request =>
    this.competitions.remove(request.competition)
    Redirect(self.showManager()).withSuccess("success.deleted.competition")
  }

  /**
    * Displays the project entries in the specified competition.
    *
    * @param id Competition ID
    * @return   List of project entries
    */
  def showProjects(id: Int, page: Option[Int]) = CompetitionAction(id) { implicit request =>
    Ok(views.projects.competitions.projects(request.competition, page.getOrElse(1), 25))
  }

}
