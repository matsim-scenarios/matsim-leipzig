countdrivenlinks <- function(movements, fromstopID,tostop_ID,tsvname){

  sortedMovement <- movements[order(movements$requested_origin_assigned_flexa_stop,movements$requested_destination_assigned_flexa_stop), ]

  #erstellt Vektoren / vll noch eone schönere Lösung möglich
  laenge <- length(movements$requested_origin_assigned_flexa_stop)
  fromLink <-character(0)
  toLink <- character(0)
  anzahlFahrten <- character(0)
  fromstopIds <- character(0)
  tostopIds <- character(0)


  Fahrten <- 1 # counter of Trips from one to another stop
  connection <- c(sortedMovement[1,fromstopID],sortedMovement[1,tostop_ID]) # Vektor mit einem from und einem to Link drin

  #Iteration durch sorted Movement, wobei paarweise die Tuple (hier Vektoren, connection und newConnection) verglichen werden
  # sind sie identisch, wird Fahrten+1 gerechnet, ansonsten werden di eTuple abgespeichert und das nächste Tupel wird verglichen
  for(row in 2:laenge){
    newConnection <- c(sortedMovement[row,fromstopID],sortedMovement[row,tostop_ID]) # nächster Vektor mit einem from und einem to Link drin
    if (identical(connection,newConnection)){
      Fahrten <- Fahrten + 1
    }
    else{
      fromLink <- c(fromLink,connection[1])
      toLink <- c(toLink,connection[2])
      anzahlFahrten <- c(anzahlFahrten,Fahrten)
      Fahrten <- 1
      fromstopIds <- c(fromstopIds,as.character(connection[1]))
      tostopIds <- c(tostopIds,as.character(connection[2]))


      connection <- newConnection

    }
  }

  #mögliche letztes Tupel abfangen
  connection <- newConnection
  fromLink <- c(fromLink,connection[1])
  toLink <- c(toLink,connection[2])
  anzahlFahrten <- c(anzahlFahrten,Fahrten)
  fromstopIds <- c(fromstopIds,as.character(connection[1]))
  tostopIds <- c(tostopIds,as.character(connection[2]))

  # in Datadfame speichern und als csv Datei abspeichern

  #class.df <- data.frame(fromstopIds,tostopIds,fromlat,fromlon,tolat,tolon,anzahlFahrten,stringsAsFactors = FALSE)
  class.smalldf <- data.frame(fromstopIds,tostopIds,anzahlFahrten)

  setwd(workDir)
  print(tsvname)

  #write.csv2(class.smalldf,csvfilename,quote=FALSE, row.names=FALSE)
  write.table(class.smalldf,tsvname,quote=FALSE, sep=";",row.names = F)

}
