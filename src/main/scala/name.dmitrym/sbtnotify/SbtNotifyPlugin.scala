// Copyright 2016 Dmitry Melnichenko.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package name.dmitrym.sbtnotify

import java.io.IOException

import sbt._
import Keys._

import scala.annotation.tailrec
import scala.sys.process.Process

object SbtNotifyPlugin extends AutoPlugin {
  object autoImport {
    val nt = taskKey[Unit]("Compile or test with result notification")
  }
  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override lazy val projectSettings = inConfig(Compile)(
      Seq(
        nt := ntCompileTask.value
      )) ++ inConfig(Test)(
      Seq(
        nt := ntTestTask.value
      ))

  lazy val ntCompileTask = Def.task {
    implicit val l = streams.value.log
    executeTasks(name.value, Seq(("Compile", compile.result.value)))
  }

  lazy val ntTestTask = Def.task {
    implicit val l = streams.value.log
    executeTasks(name.value,
                 Seq(
                   ("Compile", compile.result.value),
                   ("Test", test.result.value)
                 ))
  }

  @tailrec
  private[this] def executeTasks(
      project: String,
      tasks: Seq[(String, Result[_])])(implicit l: Logger): Unit = {
    tasks match {
      case h :: Nil => // Last task in queue
        h._2 match {
          case _: Inc => notifyFail(l)(project, h._1)
          case _: Value[_] => notifySuccess(l)(project, h._1)
        }
      case h :: t => // Something in queue
        h._2 match {
          case _: Inc => notifyFail(l)(project, h._1)
          case _: Value[_] => executeTasks(project, t)
        }
    }
  }

  lazy val successPair: (Char, String) = ('\u2714', "Glass")
  lazy val failPair: (Char, String) = ('\u2717', "Glass")

  private[this] def notify(l: Logger)(
      success: Boolean)(project: String, action: String): Unit = {
    sys.props("os.name") match {
      case "Mac OS S" => notifyOSX(success, project, action)
      case "Linux" => notifyLinux(success, project, action)
      case other => l.error(s"$other platform is not supported yet")
    }
  }
  private[this] def notifySuccess(l: Logger) = notify(l)(success = true) _
  private[this] def notifyFail(l: Logger) = notify(l)(success = false) _
  private[this] def notifyOSX(success: Boolean,
                              project: String,
                              action: String): Unit = {
    val (resultMark, resultSound) = if (success) successPair else failPair
    Process(
      Seq(
        "osascript",
        "-s",
        "o",
        "-e",
        "display notification \"" + action + " completed!\" with title \"" + resultMark + " " + action + "\" subtitle \"" + project + "\" sound name \"" + resultSound + "\"")
    ).!
  }
  private[this] def notifyLinux(success: Boolean,
                                project: String,
                                action: String): Unit = {
    val (resultMark, _) = if (success) successPair else failPair
    try {
      Process(
        Seq(
          "notify-send",
          "-a",
          project,
          "-i",
          if (success) "dialog-information" else "dialog-error",
          project + " => " + action + " " + resultMark,
          action + " completed!"
        )
      ).!
    } catch {
      case e: IOException =>
    }
  }
}
