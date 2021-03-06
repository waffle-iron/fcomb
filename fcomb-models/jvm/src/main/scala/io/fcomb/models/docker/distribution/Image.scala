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

package io.fcomb.models.docker.distribution

import io.fcomb.models.{ModelWithAutoLongPk, OwnerKind}
import java.time.ZonedDateTime

final case class Image(
    id: Option[Long],
    name: String,
    slug: String,
    ownerId: Long,
    ownerKind: OwnerKind,
    visibilityKind: ImageVisibilityKind,
    description: String,
    createdAt: ZonedDateTime,
    updatedAt: Option[ZonedDateTime]
) extends ModelWithAutoLongPk {
  def withPk(id: Long) = this.copy(id = Some(id))
}

object Image {
  val nameRegEx =
    """(?:(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])(?:(?:\.(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]))+)?(?::[0-9]+)?/)?[a-z0-9]+(?:(?:(?:[._]|__|[-]*)[a-z0-9]+)+)?(?:(?:/[a-z0-9]+(?:(?:(?:[._]|__|[-]*)[a-z0-9]+)+)?)+)?""".r
}

final case class DistributionImageCatalog(
    repositories: Seq[String]
)
