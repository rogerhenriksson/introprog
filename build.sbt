import sbt._
import Process._
import Keys._


lazy val hello = taskKey[Unit]("Prints welcome message")

hello := {
  println("===== WELCOME to the sbt build of lunduniversity/introprog =====")
  println("\nHAVE PATIENCE: full build can take >500sec on a 2.5GHz machine...\n")
}

lazy val commonSettings = Seq(
  organization := "se.lth.cs",
  version := "2017-snapshot",
  scalaVersion := "2.12.3"
)

lazy val plan = (project in file("plan")).
  settings(commonSettings: _*).
  settings(
    name := "plan",
    EclipseKeys.skipProject := true
  )

lazy val quiz = (project in file("quiz")).
  settings(commonSettings: _*).
  settings(
    name := "quiz",
    EclipseKeys.skipProject := true
  )

lazy val workspace = (project in file("workspace")).
  settings(commonSettings: _*).
  settings(
    name := "workspace",
    EclipseKeys.withSource := true
  )

lazy val build = taskKey[Unit]("plan/run before pdf")

lazy val gen = taskKey[Unit]("alias for plan/run")
gen := (run in Compile in plan).toTask("").value

lazy val genquiz = taskKey[Unit]("alias for quiz/run")
genquiz := (run in Compile in quiz).toTask("").value

// ************** cmd util functions

def runPdfLatexCmd(texFile: File, workDir: File, stdOutSuffix: String = "-console.log"): Unit = {
  println(s"\n ******* Compiling $texFile to pdf *******")
  val cmd = Process(
    Seq("pdflatex","-halt-on-error", texFile.getName),
    workDir
  )
  val cmdOutputFile =  workDir / texFile.getName.replace(".tex", stdOutSuffix)
  // val bibtexCmd = Process(Seq("bibtex", texFile.getName.replace(".tex", ".aux")), workDir)

  // run pdflatex command TWICE in sequence to generate toc from .aux etc:
  //val exitValue = cmd.#>(cmdOutputFile).#&&(cmd).#>(cmdOutputFile).run.exitValue
  val exitValue = cmd.#>(cmdOutputFile).run.exitValue
  if (exitValue != 0) {
    println("*** ############ ERROR LOG STARTS HERE ############### ***")
    //Process(Seq("cat", cmdOutputFile.getName), workDir).run
    Process(Seq("tail", "-40", cmdOutputFile.getName), workDir).run
    sys.error(s"\n*** ERROR: pdflatex exit code: $exitValue\nSee COMPLETE pdflatex output in: $cmdOutputFile")
  } else println(s"         Log file: $cmdOutputFile")
}

// **************

lazy val pdf = taskKey[Unit]("Compile slides and compendium using pdflatex")

pdf := {
  println("\n=== Compiling slides to pdf")
  val workDir = file("slides")
  val texFiles = (workDir * "*.tex").get
  for (texFile <- texFiles) {
    runPdfLatexCmd(texFile, workDir)
  }

  println("\n=== The main doc with all stuff in one pdf optimized for screen:")
  runPdfLatexCmd(texFile = file("compendium.tex"), workDir = file("compendium"))

  println("\n=== Docs optimized for print, two times for external xref to work:")
  runPdfLatexCmd(texFile = file("compendium1.tex"), workDir = file("compendium"))
  runPdfLatexCmd(texFile = file("compendium2.tex"), workDir = file("compendium"))

  runPdfLatexCmd(texFile = file("compendium1.tex"), workDir = file("compendium"))
  runPdfLatexCmd(texFile = file("compendium2.tex"), workDir = file("compendium"))

  runPdfLatexCmd(texFile = file("lectures.tex"),  workDir = file("compendium"))
  runPdfLatexCmd(texFile = file("exercises.tex"), workDir = file("compendium"))
  runPdfLatexCmd(texFile = file("labs.tex"),      workDir = file("compendium"))

  runPdfLatexCmd(texFile = file("lectures.tex"),  workDir = file("compendium"))
  runPdfLatexCmd(texFile = file("exercises.tex"), workDir = file("compendium"))
  runPdfLatexCmd(texFile = file("labs.tex"),      workDir = file("compendium"))

  runPdfLatexCmd(texFile = file("assignments.tex"), workDir = file("compendium"))
  runPdfLatexCmd(texFile = file("solutions.tex"), workDir = file("compendium"))
}

//http://www.scala-sbt.org/0.13/docs/Howto-Triggered.html
watchSources ++= ((baseDirectory.value / "compendium") * "*.tex").get
watchSources ++= ((baseDirectory.value / "compendium") * "*.cls").get

watchSources ++= ((baseDirectory.value / "compendium" / "modules") * "*.tex").get

lazy val pdfExercises = taskKey[Unit]("Compile exercises.tex")
pdfExercises := {
  runPdfLatexCmd(texFile = file("exercises.tex"), workDir = file("compendium"))
}

lazy val pdfSolutions = taskKey[Unit]("Compile solutions.tex")
pdfSolutions := {
  runPdfLatexCmd(texFile = file("solutions.tex"), workDir = file("compendium"))
}

lazy val pdfLabs = taskKey[Unit]("Compile labs.tex")
pdfLabs := {
  runPdfLatexCmd(texFile = file("labs.tex"), workDir = file("compendium"))
}


lazy val pdfCompendium = taskKey[Unit]("Compile compendium.tex")
pdfCompendium := {
  runPdfLatexCmd(texFile = file("compendium.tex"), workDir = file("compendium"))
}


lazy val root = (project in file(".")).
  aggregate(workspace, plan).
  settings(commonSettings: _*).
  settings(
    name := "introprog",
    build := Def.sequential(
      hello,
      (run in Compile in plan).toTask(""),
//    (run in Compile in quiz).toTask(""),  //decomment if you want re-scrambled quizes
      pdf
    ).value
  )

// ***********************************************************
