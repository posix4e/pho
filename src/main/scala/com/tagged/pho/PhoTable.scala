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

import com.tagged.pho.converter.IdentityConverter
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import scala.collection.JavaConverters._

class PhoTable(connection: HConnection, tableName: String) {

  def withTable[A](block: HTableInterface => A) = {
    val table = connection.getTable(tableName)
    try {
      block(table)
    } finally {
      table.close()
    }
  }

  def withScanner[A](scan: Scan)(block: Seq[Result] => A) = {
    withTable { table =>
      val scanner = table.getScanner(scan)
      try {
        block(scanner.asScala.toSeq)
      } finally {
        scanner.close()
      }
    }
  }

  def write(doc: Document[_]) = {
    withTable { table =>
      table.put(doc.getPut)
    }
  }

  def read[A](rowKey: RowKey[A], columns: Seq[Column[_]]): Document[A] = {
    withTable { table =>
      val get = new Get(rowKey.toBytes)
      for (column <- columns) {
        get.addColumn(column.family.bytes, column.qualifier.bytes)
      }
      val result = table.get(get)
      val cells = columns.map(_.getCell(result).orNull).filter(_ != null)
      Document(rowKey, cells)
    }
  }

  def read[A](query: Query[A]): Seq[Document[A]] = {
    val map: Map[ColumnFamily,Map[Qualifier,Column[_]]] = query.columns
      .groupBy(_.family)
      .mapValues(_.map({ column =>
        column.qualifier -> column
      }).toMap)
    withScanner(query.getScan) { scanner =>
      scanner.map({ result =>
        val key = query.startRow.getRowKey(result)
        val cells = for ((familyBytes: Array[Byte], qualifiers) <- result.getMap.asScala) yield {
          val family = ColumnFamily(familyBytes)
          for ((qualifierBytes: Array[Byte], values) <- qualifiers.asScala) yield {
            val qualifier = Qualifier(qualifierBytes)
            val column = map.get(family) match {
              case Some(columns) => columns.get(qualifier) match {
                case Some(column) => column
                case None => Column(family, qualifier, IdentityConverter)
              }
              case None => Column(family, qualifier, IdentityConverter)
            }
            for ((version: java.lang.Long, valueBytes: Array[Byte]) <- values.asScala) yield {
              column.getCell(valueBytes)
            }
          }
        }
        Document(key, cells.flatten.flatten.toSeq)
      })
    }
  }

}
