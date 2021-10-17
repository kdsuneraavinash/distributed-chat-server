#!/bin/bash

Build() {
  ./gradlew shadowJar -q
}

Run() {
  LOG4J_LEVEL="$1" java -jar build/libs/chat-lk.ac.mrt.cse.cs4262.server-1.0-SNAPSHOT-all.jar ${@:2}
}

Client() {
  java -jar assets/client.jar ${@:1}
}

Help() {
  echo "Scripts to manage/run the project."
  echo
  echo "Syntax: ./manage.sh (run|trace|build|buildrun|check) [arguments]"
  echo "  run         Run the chat server current build."
  echo "  trace       Run the chat server current build in trace mode."
  echo "  build       Build the chat server."
  echo "  buildrun    Build and run the chat server."
  echo "  check       Check source for linter errors."
  echo "  client      Run chat client with given arguments."
  echo
}

if [ "$1" == "run" ]; then
  Run INFO ${@:2}

elif [ "$1" == "rundefault" ]; then
  Run INFO -s ${@:2} -f default.tsv

elif [ "$1" == "trace" ]; then
  Run TRACE ${@:2}

elif [ "$1" == "build" ]; then
  Build

elif [ "$1" == "buildrun" ]; then
  Build && Run INFO ${@:2}

elif [ "$1" == "check" ]; then
  ./gradlew build checkStyleMain

elif [ "$1" == "client" ]; then
  Client ${@:2}

else
  Help

fi
