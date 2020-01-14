  def convertPrivateURLToSshProtocol(url: String): String = {
    val absolutePath = """^[-a-zA-Z0-9_\.]+@[-a-zA-Z0-9_\.]+(?::\d+)?\/.*""".r
    url match {
      case absolutePath() => "ssh://" + url
      case _              => url
    }
  }
