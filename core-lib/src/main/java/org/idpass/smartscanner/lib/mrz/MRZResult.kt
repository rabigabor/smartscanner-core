/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.idpass.smartscanner.lib.mrz

import org.idpass.smartscanner.lib.platform.utils.DateUtils
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRecord
import org.idpass.smartscanner.mrz.parser.innovatrics.records.MrtdTd1


val DriverLicenseCheckPairs = arrayOf(
  arrayOf("1.", "surname"),
  arrayOf("1,", "surname"),
  arrayOf("2.", "givenNames"),
  arrayOf("2,", "givenNames"),
  arrayOf("3.", "dateOfBirth"),
  arrayOf("3,", "dateOfBirth"),
  arrayOf("5.", "documentNumber"),
  arrayOf("5,", "documentNumber"),
  arrayOf("4(b).", "expirationDate"),
  arrayOf("4(b),", "expirationDate"),
  arrayOf("4(b)", "expirationDate"),
  arrayOf("4 b).", "expirationDate"),
  arrayOf("4 b),", "expirationDate"),
  arrayOf("4 b)", "expirationDate"),
  arrayOf("4b).", "expirationDate"),
  arrayOf("4b),", "expirationDate"),
  arrayOf("4b)", "expirationDate"),
  arrayOf("4b.", "expirationDate"),
  arrayOf("4b,", "expirationDate"),
  arrayOf("4b", "expirationDate"),
)

data class DriverLicenseRecord(
  var dateOfBirth: String? = null,
  var expirationDate: String? = null,
  var documentNumber: String? = null,
  var nationality: String? = "HUN",
  var issuingCountry: String? = "HUN",
  var surname: String? = null,
  var givenNames: String? = null
) {

  operator fun set(field: String, value: String) {
    when(field){
      "dateOfBirth" -> this.dateOfBirth = value
      "expirationDate" -> this.expirationDate = value
      "documentNumber" -> this.documentNumber = value
      "surname" -> this.surname = value
      "givenNames" -> this.givenNames = value
      else -> null
    }
  }
}

data class MRZResult(
        val image: String?,
        val code: String?,
        val code1: Short?,
        val code2: Short?,
        val dateOfBirth: String?,
        val documentNumber: String?,
        val expirationDate: String?,
        val format: String?,
        val givenNames: String?,
        val issuingCountry: String?,
        val nationality: String?,
        val sex: String?,
        val surname: String?,
        var mrz: String?,
        val optional: String? = null,
        val optional2: String? = null,
        val validComposite: Boolean? = null,
        val rawAll: String? = null
) {
    companion object {
        fun formatMrzResult(record: MrzRecord, image: String? = "", rawAll: String? = "") : MRZResult {
            val dateOfBirth = DateUtils.toAdjustedDate(record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""))
            val expirationDate = DateUtils.toReadableDate(record.expirationDate?.toString()?.replace(Regex("[{}]"), ""))
            return MRZResult(
                    image = image,
                    code = record.code.toString(),
                    code1 = record.code1.code.toShort(),
                    code2 = record.code2.code.toShort(),
                    dateOfBirth = dateOfBirth,
                    documentNumber = record.documentNumber.toString(),
                    expirationDate = expirationDate,
                    format = record.format.toString(),
                    givenNames = record.givenNames,
                    issuingCountry = record.issuingCountry,
                    nationality = record.nationality,
                    sex = record.sex.toString(),
                    surname = record.surname,
                    mrz = record.toMrz(),
                    validComposite = record.validComposite,
                    rawAll = rawAll
            )
        }

        fun formatMrtdTd1Result(record: MrtdTd1, image: String?, rawAll: String? = "") : MRZResult {
            val dateOfBirth = DateUtils.toAdjustedDate(record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""))
            val expirationDate = DateUtils.toReadableDate(record.expirationDate?.toString()?.replace(Regex("[{}]"), ""))
            return MRZResult(
                    image = image,
                    code = record.code.toString(),
                    code1 = record.code1.code.toShort(),
                    code2 = record.code2.code.toShort(),
                    dateOfBirth = dateOfBirth,
                    documentNumber = record.documentNumber.toString(),
                    expirationDate = expirationDate,
                    format = record.format.toString(),
                    givenNames = record.givenNames,
                    issuingCountry = record.issuingCountry,
                    nationality = record.nationality,
                    sex = record.sex.toString(),
                    surname = record.surname,
                    mrz = record.toMrz(),
                    optional = record.optional,
                    optional2 = record.optional2,
                    validComposite = record.validComposite,
                    rawAll = rawAll
            )
        }

        fun formatDriverLicenseResult(record: DriverLicenseRecord?, image: String?, rawAll: String? = ""): MRZResult {
          val dateOfBirth = record?.dateOfBirth?.trim('.', ',', ' ')?.replace(',', '-')?.replace('.', '-')
          val expirationDate = record?.expirationDate?.trim('.', ',', ' ')?.replace(',', '-')?.replace('.', '-')
          return MRZResult(
            image = image,
            dateOfBirth = dateOfBirth,
            documentNumber = record?.documentNumber?.replace(" ", ""),
            expirationDate = expirationDate,
            givenNames = record?.givenNames,
            issuingCountry = record?.issuingCountry,
            nationality = record?.nationality,
            surname = record?.surname,
            validComposite = true,
            rawAll = rawAll,
            code = null,
            code1 = null,
            code2 = null,
            format = "driver_license",
            mrz = null,
            optional = null,
            optional2 = null,
            sex = null
          )
        }
    }
}
