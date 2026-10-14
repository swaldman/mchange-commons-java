package com.mchange.commons.docsite

case class MainLayoutInput( renderLocation : DocSite#SiteLocation, version : String )

def gen(version : String, staticRootDir : os.Path, outDir : os.Path) : Unit =
  import zio.*
  Unsafe.unsafely {
    val site = new DocSite(version, staticRootDir.toNIO)
    Runtime.default.unsafe.run(unstatic.ztapir.ZTStaticGen.generateZTSite(site,outDir.toNIO)).getOrThrow()
  }
