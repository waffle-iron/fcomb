/*
 * Copyright 2016 fcomb. <https://fcomb.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fcomb.frontend.components.dashboard

import cats.data.Xor
import io.fcomb.frontend.DashboardRoute
import io.fcomb.frontend.api.{Rpc, RpcMethod, Resource}
import io.fcomb.json.rpc.docker.distribution.Formats._
import io.fcomb.models.docker.distribution.ImageVisibilityKind
import io.fcomb.rpc.docker.distribution.{ImageResponse, ImageCreateRequest}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object NewRepositoryComponent {
  final case class State(name: String,
                         visibilityKind: ImageVisibilityKind,
                         description: Option[String],
                         isFormDisabled: Boolean)

  final case class Backend($ : BackendScope[RouterCtl[DashboardRoute], State]) {
    def create(ctl: RouterCtl[DashboardRoute]): Callback = {
      $.state.flatMap { state =>
        if (state.isFormDisabled) Callback.empty
        else {
          Callback.future {
            val req = ImageCreateRequest(state.name, state.visibilityKind, state.description)
            Rpc
              .callWith[ImageCreateRequest, ImageResponse](RpcMethod.POST,
                                                           Resource.userRepositories,
                                                           req)
              .map {
                case Xor.Right(_) =>
                  Callback.empty
                case Xor.Left(e) =>
                  Callback.empty
              }
          }
        }
      }
    }

    def handleOnSubmit(ctl: RouterCtl[DashboardRoute])(e: ReactEventH): Callback = {
      e.preventDefaultCB >> create(ctl)
    }

    def updateName(e: ReactEventI): Callback = {
      val value = e.target.value
      $.modState(_.copy(name = value))
    }

    def updateVisibilityKind(e: ReactEventI): Callback = {
      val value = ImageVisibilityKind.withName(e.target.value)
      $.modState(_.copy(visibilityKind = value))
    }

    def updateDescription(e: ReactEventI): Callback = {
      val value = e.target.value match {
        case s if s.nonEmpty => Some(s)
        case _               => None
      }
      $.modState(_.copy(description = value))
    }

    def render(ctl: RouterCtl[DashboardRoute], state: State) = {
      <.div(
        <.h2("New repository"),
        <.form(
          ^.onSubmit ==> handleOnSubmit(ctl),
          ^.disabled := state.isFormDisabled,
          <.label(^.`for` := "name", "Name"),
          <.input.text(^.id := "name",
                       ^.name := "name",
                       ^.autoFocus := true,
                       ^.required := true,
                       ^.tabIndex := 1,
                       ^.value := state.name,
                       ^.onChange ==> updateName),
          <.br,
          <.label(^.`for` := "visibilityKind", "Visibility"),
          <.select(^.id := "visibilityKind",
                   ^.name := "visibilityKind",
                   ^.required := true,
                   ^.tabIndex := 2,
                   ^.value := state.visibilityKind.value,
                   ^.onChange ==> updateVisibilityKind,
                   ImageVisibilityKind.values.map(k => <.option(^.value := k.value)(k.entryName))),
          <.br,
          <.label(^.`for` := "description", "Description"),
          <.textarea(^.id := "description",
                     ^.name := "description",
                     ^.tabIndex := 3,
                     ^.value := state.description.getOrElse(""),
                     ^.onChange ==> updateDescription),
          <.br,
          <.input.submit(^.tabIndex := 4, ^.value := "Create"))
      )
    }
  }

  private val component = ReactComponentB[RouterCtl[DashboardRoute]]("NewRepositoryComponent")
    .initialState(State("", ImageVisibilityKind.Private, None, false))
    .renderBackend[Backend]
    .build

  def apply(ctl: RouterCtl[DashboardRoute]) = component(ctl)
}
