package com.mchange.commons.docsite

import scala.collection.*

import unstatic.*, UrlPath.*
import unstatic.ztapir.*

import java.nio.file.Path as JPath

/**
 *  The mchange-commons-java microsite: one page, a gateway to the javadocs.
 *
 *  Replaces a Scala paradox build that no longer exists. The page it generates
 *  is deliberately as spare as the paradox one was -- name, tagline, version,
 *  and a link to the API documentation that the build grafts in alongside it as
 *  `apidocs/`.
 *
 *  The static root is resolved against the process working directory, so the
 *  generator must be run from the `doc` module directory. The build's `genSite`
 *  task sets `cwd` accordingly.
 */
object DocSite extends ZTSite.SingleStaticRootComposite( JPath.of("static") ):
  // serverUrl must end in a slash. basePath is where the "current" symlink
  // lands, not the versioned directory we stage into -- only absolute URLs in
  // generated output would notice the difference, and this page emits none.
  override val serverUrl : Abs    = Abs("https://www.mchange.com/")
  override val basePath  : Rooted = Rooted("/projects/mchange-commons-java")

  // supplied by the build, which knows publishVersion; see genSite
  val Version = sys.props.getOrElse("mchange-commons-java.version", "unknown")

  case class MainLayoutInput( renderLocation : SiteLocation, version : String )

  object IndexPage extends ZTEndpointBinding.Source:
    val location = DocSite.location("/index.html")
    val task     = zio.ZIO.attempt( layout_main_html( MainLayoutInput( location, Version ) ).text )
    val endpointBindings =
      ZTEndpointBinding.publicReadOnlyHtml( location, task, None, immutable.Set("index","home") ) :: Nil
  end IndexPage

  override val endpointBindingSources : immutable.Seq[ZTEndpointBinding.Source] = immutable.Seq( IndexPage )

object DocSiteGenerator extends ZTMain(DocSite, "mchange-commons-java-docsite")
