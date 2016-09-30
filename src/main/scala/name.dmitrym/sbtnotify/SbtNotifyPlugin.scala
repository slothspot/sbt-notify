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
    executeTasks(name.value, Seq(("Compile", compile.result.value)))
  }

  lazy val ntTestTask = Def.task {
    executeTasks(name.value,
                 Seq(
                   ("Compile", compile.result.value),
                   ("Test", test.result.value)
                 ))
  }

  @tailrec
  private[this] def executeTasks(project: String,
                                 tasks: Seq[(String, Result[_])]): Unit = {
    tasks match {
      case h :: Nil => // Last task in queue
        h._2 match {
          case _: Inc => notifyFail(project, h._1)
          case _: Value[_] => notifySuccess(project, h._1)
        }
      case h :: t => // Something in queue
        h._2 match {
          case _: Inc => notifyFail(project, h._1)
          case _: Value[_] => executeTasks(project, t)
        }
    }
  }

  lazy val successPair: (Char, String) = ('\u2714', "Glass")
  lazy val failPair: (Char, String) = ('\u2717', "Glass")

  private[this] def notify(successful: Boolean)(project: String,
                                                action: String): Unit = {
    val (resultMark, resultSound) = if (successful) successPair else failPair
    Process(
      Seq(
        "osascript",
        "-s",
        "o",
        "-e",
        "display notification \"" + action + " completed!\" with title \"" + resultMark + " " + action + "\" subtitle \"" + project + "\" sound name \"" + resultSound + "\"")
    ).!
  }
  private[this] def notifySuccess = notify(successful = true) _
  private[this] def notifyFail = notify(successful = false) _
}
