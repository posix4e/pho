/*
 * Copyright 2014 Tagged
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

package com.tagged.pho

/**
 * A grouping of columns,
 * contained within a shared unit of physical storage.
 */
class ColumnFamily(bytes: Array[Byte]) extends Identifier(bytes) {

  def this(name: String) = this(Identifier.bytesFromString(name))

  override def toString: String = super.toString

}

object ColumnFamily {

  def apply(bytes: Array[Byte]): ColumnFamily = new ColumnFamily(bytes)
  def apply(name: String): ColumnFamily = new ColumnFamily(name)

}
