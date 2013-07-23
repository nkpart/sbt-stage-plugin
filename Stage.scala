import sbt._

import Keys._

object StageKeys {
  val stage = TaskKey[Unit]("stage")

  val packageForStage = TaskKey[Seq[File]]("package-for-stage")

  val stageMainAndArgs = SettingKey[String]("stage-main-and-args")

  val packageExcludes = SettingKey[Seq[String]]("package-excludes")
}

object Stage extends Plugin {
  import StageKeys._
  def defaultSettings: Seq[Project.Setting[_]] = Seq(
    stage <<= playStageTask,
    packageExcludes := Seq(),
    packageForStage <<= packageForStageTask
  )

  val playStageTask = (baseDirectory, packageForStage, stageMainAndArgs, dependencyClasspath in Runtime, target, streams).map { (root,
    packaged, mainAndArgs, dependencies, target, s) =>
    import sbt.NameFilter._

    val staged = target / "staged"

    IO.delete(staged)
    IO.createDirectory(staged)

    val libs = dependencies.filter(_.data.ext == "jar").map(_.data) ++ packaged

    libs.foreach { jar =>
      IO.copyFile(jar, new File(staged, jar.getName))
    }

    val start = target / "start"
    IO.write(start,
      """|#!/usr/bin/env sh
         |
         |exec java $@ -cp "`dirname $0`/staged/*" %s
         |""".stripMargin format mainAndArgs)

    "chmod a+x %s".format(start.getAbsolutePath) !

    s.log.info("")
    s.log.info("Your application is ready to be run in place: target/start")
    s.log.info("")

    ()
  }

  val packageForStageTask = (state, thisProjectRef, packageExcludes).flatMap { (state, project, excludes) =>
    def taskInAllDependencies[T](taskKey: TaskKey[T]): Task[Seq[T]] =
      inAllDependencies(project, taskKey.task, Project structure state).join

    for {
      packaged <- taskInAllDependencies(packagedArtifacts)
      srcs <- taskInAllDependencies(packageSrc in Compile)
      docs <- taskInAllDependencies(packageDoc in Compile)
    } yield {
      val allJars: Seq[Iterable[File]] = for {
        artifacts: Map[Artifact, File] <- packaged
      } yield {
        artifacts
          .filter { case (artifact, _) => artifact.extension == "jar" && !excludes.contains(artifact.name) }
          .map { case (_, path) => path }
      }
      allJars
        .flatten
        .diff(srcs ++ docs) //remove srcs & docs since we do not need them in the dist
        .distinct
    }
  }

  // -- Utility methods for 0.10-> 0.11 migration
  def inAllDependencies[T](base: ProjectRef, key: SettingKey[T], structure: Load.BuildStructure): Seq[T] = {
    def deps(ref: ProjectRef): Seq[ProjectRef] =
      Project.getProject(ref, structure).toList.flatMap { p =>
        p.dependencies.map(_.project) ++ p.aggregate
      }
    inAllDeps(base, deps, key, structure.data)
  }

  def inAllDeps[T](base: ProjectRef, deps: ProjectRef => Seq[ProjectRef], key: SettingKey[T], data: Settings[Scope]): Seq[T] =
    inAllProjects(Dag.topologicalSort(base)(deps), key, data)

  def inAllProjects[T](allProjects: Seq[Reference], key: SettingKey[T], data: Settings[Scope]): Seq[T] =
    allProjects.flatMap { p => key in p get data }
}
