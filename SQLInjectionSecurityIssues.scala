private def testDatabase(dbname: String, url: String, user: String, password: String): Response[Boolean] = {
    testQuery(dbname, url, user, password)(sql"SELECT * FROM play_evolutions LIMIT 1;")
  }
