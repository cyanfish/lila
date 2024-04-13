package views.html.user

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ScalatagsTemplate.{ *, given }
import lila.core.perf.UserWithPerfs
import lila.user.Profile.flagInfo
import lila.rating.UserPerfsExt.best8Perfs

object mini:

  def apply(
      u: UserWithPerfs,
      playing: Option[lila.game.Pov],
      blocked: Boolean,
      followable: Boolean,
      rel: Option[lila.relation.Relation],
      ping: Option[Int],
      crosstable: Option[lila.game.Crosstable]
  )(using ctx: Context) =
    frag(
      div(cls := "upt__info")(
        div(cls := "upt__info__top")(
          userLink(u, withPowerTip = false),
          u.profileOrDefault.flagInfo.map: c =>
            val titleNameSize      = u.title.fold(0)(_.value.length + 1) + u.username.length
            val hasRoomForNameText = titleNameSize + c.shortName.length < 21
            span(
              cls   := "upt__info__top__flag",
              title := (!hasRoomForNameText).option(c.name)
            )(
              img(cls := "flag", src := assetUrl(s"images/flags/${c.code}.png")),
              hasRoomForNameText.option(c.shortName)
            )
          ,
          ping.map(bits.signalBars)
        ),
        if u.lame && ctx.isnt(u) && !isGranted(_.UserModView)
        then div(cls := "upt__info__warning")(trans.site.thisAccountViolatedTos())
        else
          ctx.pref.showRatings.option(div(cls := "upt__info__ratings"):
            u.perfs.best8Perfs.map(showPerfRating(u.perfs, _))
          )
      ),
      ctx.userId.map: myId =>
        frag(
          (myId.isnt(u.id) && u.enabled.yes).option(
            div(cls := "upt__actions btn-rack")(
              a(
                dataIcon := Icon.AnalogTv,
                cls      := "btn-rack__btn",
                title    := trans.site.watchGames.txt(),
                href     := routes.User.tv(u.username)
              ),
              (!blocked).option(
                frag(
                  a(
                    dataIcon := Icon.BubbleSpeech,
                    cls      := "btn-rack__btn",
                    title    := trans.site.chat.txt(),
                    href     := routes.Msg.convo(u.username)
                  ),
                  a(
                    dataIcon := Icon.Swords,
                    cls      := "btn-rack__btn",
                    title    := trans.challenge.challengeToPlay.txt(),
                    href     := s"${routes.Lobby.home}?user=${u.username}#friend"
                  )
                )
              ),
              views.html.relation.mini(u.id, blocked, followable, rel)
            )
          ),
          crosstable
            .flatMap(_.nonEmpty)
            .map: cross =>
              a(
                cls   := "upt__score",
                href  := s"${routes.User.games(u.username, "me")}#games",
                title := trans.site.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
              ):
                trans.site.yourScore(raw:
                  val opponent = ~cross.showOpponentScore(myId)
                  s"""<strong>${cross.showScore(myId)}</strong> - <strong>$opponent</strong>"""
                )
        ),
      isGranted(_.UserModView).option(
        div(cls := "upt__mod")(
          span(
            trans.site.nbGames.plural(u.count.game, u.count.game.localize),
            " ",
            momentFromNowOnce(u.createdAt)
          ),
          (u.lameOrTroll || u.enabled.no).option(span(cls := "upt__mod__marks")(mod.userMarks(u.user, None)))
        )
      ),
      playing.map(views.html.game.mini(_))
    )
