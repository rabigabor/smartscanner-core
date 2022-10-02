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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.idpass.smartscanner.api.ScannerConstants
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.platform.BaseImageAnalyzer
import org.idpass.smartscanner.lib.platform.extension.*
import org.idpass.smartscanner.lib.platform.utils.BitmapUtils
import org.idpass.smartscanner.lib.scanner.config.ImageResultType
import org.idpass.smartscanner.lib.scanner.config.Modes
import org.idpass.smartscanner.lib.scanner.config.MrzFormat
import java.io.File
import java.net.URLEncoder

open class MRZAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    override val mode: String = Modes.MRZ.value,
    private val language: String? = null,
    private val label: String? = null,
    private val locale: String? = null,
    private val withPhoto: Boolean? = null,
    private val withMrzPhoto: Boolean? = null,
    private val captureLog: Boolean? = null,
    private val enableLogging: Boolean? = null,
    private val isMLKit: Boolean,
    private val imageResultType: String,
    private val format: String?,
    private val analyzeTime: Int = 5000,
    private val analyzeStart: Long
) : BaseImageAnalyzer() {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        bitmap?.let { bf ->
            val rot = imageProxy.imageInfo.rotationDegrees
            bf.apply {
                // Increase brightness and contrast for clearer image to be processed
                setContrast(1.5F)
                setBrightness(5F)
            }
            val cropped = when (rot) {
                90, 270 -> {
                    Bitmap.createBitmap(
                        bf,
                        bf.width / 4,
                        // bf.width / 2, // THIS!
                        0,
                        bf.width / 2,
                        bf.height
                    )
                }
                180 -> Bitmap.createBitmap(bf, 0, bf.height / 4, bf.width, bf.height / 4)
                else -> Bitmap.createBitmap(bf, 0, bf.height / 3, bf.width, bf.height / 3)
                // 180 -> Bitmpap.createBitmap(bf, 0 , bf.height / 4, bf.width, bf.height / 2) // THIS!
                // else -> Bitma.createBitmap(bf, 0 , bf.height / 3, bf.width, bf.height / 3) // THIS!

            }
            // val cropped = if (rot == 90 || rot == 270) Bitmap.createBitmap(
            // val bf = mediaImage.toBitmap(rot, mode)
            //val cropped = bf; // THIS!
            /*val cropped = if (rot == 90 || rot == 270) Bitmap.createBitmap(
                    bf,
                    bf.width / 2,
                    0,
                    bf.width / 2,
                    bf.height
            )
            else Bitmap.createBitmap(bf, 0, bf.height / 2, bf.width, bf.height / 2) */
            Log.d(
                SmartScannerActivity.TAG,
                "Bitmap: (${bf.width}, ${bf.height} Cropped: (${cropped.width}, ${cropped.height}), Rotation: $rot"
            )

            // Pass image to an ML Kit Vision API
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit: start")
            val start = System.currentTimeMillis()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromBitmap(cropped, rotation)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit TextRecognition: process")
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val timeRequired = System.currentTimeMillis() - start

                    if (format == "driver_license"){
                      var rawAll = ""
                      val record = DriverLicenseRecord()
                      val blocks = visionText.textBlocks
                      for (i in blocks.indices) {
                        val lines = blocks[i].lines
                        for (j in lines.indices) {
                          val text = lines[j].text.trim()
                          // if (lines[j].confidence < 0.5) continue
                          rawAll += text+"\n"

                          for(pair in DriverLicenseCheckPairs){
                            val prefix = pair[0]
                            val field = pair[1]
                            if (text.startsWith(prefix)){
                              record[field] = text.removePrefix(prefix).trim()
                              break
                            }
                          }
                        }
                      }
                      Log.d("${SmartScannerActivity.TAG}/SmartScannerDL",
                        "DL_RECORD - $record"
                      )
                      record.dateOfBirth  = (record.dateOfBirth?.trim('.', ',', ' ')?.replace(",", "")?.replace(".", "")?.replace(" ", "")?.replace("/", ""))
                      record.expirationDate  = (record.expirationDate?.trim('.', ',', ' ')?.replace(",", "")?.replace(".", "")?.replace(" ", "")?.replace("/", ""))
                      if (
                        record.dateOfBirth != null && record.expirationDate != null && record.givenNames != null && record.surname != null && record.documentNumber != null &&
                          (record.dateOfBirth.toString().length == 8 || record.dateOfBirth.toString().length == 6)
                      ){
                        processResult(result = "", bitmap = bf, rotation = rotation, rawAll = rawAll, dlRecord = record)
                      }
                      imageProxy.close()
                      return@addOnSuccessListener
                    }

                    Log.d(
                        "${SmartScannerActivity.TAG}/SmartScanner",
                        "MRZ MLKit TextRecognition: success: $timeRequired ms"
                    )
                    var rawFullRead = "";
                    var rawAll = "";
                    val blocks = visionText.textBlocks;
                    var prevLine = "";
                    for (i in blocks.indices) {
                        val lines = blocks[i].lines
                        for (j in lines.indices) {
                            Log.d("${SmartScannerActivity.TAG}/SmartScanner",
                                " prevLine $prevLine (${prevLine != ""} && ${prevLine.contains('<')} && ${prevLine.length == lines[j].text.length})")
                            rawAll += lines[j].text + "\n"
                            if (lines[j].text.contains('<') ||
                                (prevLine != "" && prevLine.contains('<') && prevLine.length == lines[j].text.length)
                            ) {
                                rawFullRead += lines[j].text + "\n"
                            }
                            prevLine = lines[j].text;
                        }
                    }
                    try {
                        Log.d(
                                "${SmartScannerActivity.TAG}/SmartScanner",
                                "Before cleaner: [${
                                    URLEncoder.encode(rawFullRead, "UTF-8")
                                            .replace("%3C", "<").replace("%0A", "↩")
                                }]"
                        )

                        Log.d("${SmartScannerActivity.TAG}/SmartScanner", "Analyze time: $analyzeTime <?> ${(System.currentTimeMillis() - analyzeStart)}")

                        val cleanMRZ = MRZCleaner.clean(rawFullRead)
                        Log.d(
                                "${SmartScannerActivity.TAG}/SmartScanner",
                                "After cleaner = [${
                                    URLEncoder.encode(cleanMRZ, "UTF-8")
                                            .replace("%3C", "<").replace("%0A", "↩")
                                }]"
                        )

                        if (!cleanMRZ.startsWith("I<HUN") || (System.currentTimeMillis() - analyzeStart) > analyzeTime){
                            Log.d(
                                "${SmartScannerActivity.TAG}/SmartScanner",
                                "ignoring analyzeTime (${(System.currentTimeMillis() - analyzeStart) > analyzeTime}) or not hun (${!cleanMRZ.startsWith("I<HUN")}"
                            )
                        } else {
                            Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "still in analyzeTime and format is ok! rawAll: ${rawAll}"
                            )

                            if(
                                !rawAll.contains("Anyja", false) &&
                                !rawAll.contains("anyja", false) &&
                                !rawAll.contains("Mother", false) &&
                                !rawAll.contains("mother", false)
                            ){
                                throw IllegalArgumentException("Could not find mother's name.")
                            }else{
                                Log.d(
                                    "${SmartScannerActivity.TAG}/SmartScanner",
                                    "Check mother's name OK"
                                )
                            }
                            if(
                                !rawAll.contains("hely", false) &&
                                !rawAll.contains("place", false) &&
                                !rawAll.contains("Hely", false) &&
                                !rawAll.contains("Place", false)
                            ){
                                throw IllegalArgumentException("Could not find birth place.")
                            }else{
                                Log.d(
                                    "${SmartScannerActivity.TAG}/SmartScanner",
                                    "Check birth place OK"
                                )
                            }
                        }

                        processResult(result = cleanMRZ, bitmap = bf, rotation = rotation, rawAll = rawAll)
                        // processResult(result = cleanMRZ, bitmap = cropped, rotation = rotation, rawAll = rawAll) // THIS!
                    } catch (e: Exception) {
                        Log.d("${SmartScannerActivity.TAG}/SmartScanner", e.toString())
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    imageProxy.close()
                }
        }
    }

    internal open fun processResult(result: String, bitmap: Bitmap, rotation: Int, rawAll: String, dlRecord: DriverLicenseRecord? = null) {
        val imagePath = activity.cacheImagePath()
        bitmap.cropCenter().cacheImageToLocal(
            imagePath,
            rotation,
            if (imageResultType == ImageResultType.BASE_64.value) 40 else 80
        )
        val imageFile = File(imagePath)
        val imageString = if (imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else imagePath
        val mrz = when (format) {
            MrzFormat.MRTD_TD1.value -> MRZResult.formatMrtdTd1Result(MRZCleaner.parseAndCleanMrtdTd1(result), imageString, rawAll)
            "driver_license" -> MRZResult.formatDriverLicenseResult(dlRecord, imageString, rawAll)
            else -> MRZResult.formatMrzResult(MRZCleaner.parseAndClean(result), imageString, rawAll)
        }
        if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_MRZ_INTENT ||
            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT
        ) {
            sendBundleResult(mrzResult = mrz)
        } else {
            val jsonString = Gson().toJson(mrz)
            sendAnalyzerResult(result = jsonString)
        }
    }

    private fun sendAnalyzerResult(result: String) {
        val data = Intent()
        Log.d(SmartScannerActivity.TAG, "Success from MRZ")
        Log.d(SmartScannerActivity.TAG, "value: $result")
        data.putExtra(SmartScannerActivity.SCANNER_RESULT, result)
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    private fun sendBundleResult(mrzResult: MRZResult? = null) {
        val bundle = Bundle()
        Log.d(SmartScannerActivity.TAG, "Success from MRZ")
        mrzResult?.let { result ->
            if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_MRZ_INTENT) {
                bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, result.documentNumber)
            }
            // TODO implement proper image passing
            //  bundle.putString(ScannerConstants.MRZ_IMAGE, result.image)
            bundle.putString(ScannerConstants.MRZ_CODE, result.code)
            bundle.putShort(ScannerConstants.MRZ_CODE_1, result.code1 ?: -1)
            bundle.putShort(ScannerConstants.MRZ_CODE_2, result.code2 ?: -1)
            bundle.putString(ScannerConstants.MRZ_DATE_OF_BIRTH, result.dateOfBirth)
            bundle.putString(ScannerConstants.MRZ_DOCUMENT_NUMBER, result.documentNumber)
            bundle.putString(ScannerConstants.MRZ_EXPIRY_DATE, result.expirationDate)
            bundle.putString(ScannerConstants.MRZ_FORMAT, result.format)
            bundle.putString(ScannerConstants.MRZ_GIVEN_NAMES, result.givenNames)
            bundle.putString(ScannerConstants.MRZ_SURNAME, result.surname)
            bundle.putString(ScannerConstants.MRZ_ISSUING_COUNTRY, result.issuingCountry)
            bundle.putString(ScannerConstants.MRZ_NATIONALITY, result.nationality)
            bundle.putString(ScannerConstants.MRZ_SEX, result.sex)
            bundle.putString(ScannerConstants.MRZ_RAW, result.mrz)
        }
        bundle.putString(ScannerConstants.MODE, mode)

        val result = Intent()
        val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
            intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
        } else { "" }
        result.putExtra(ScannerConstants.RESULT, bundle)
        // Copy all the values in the intent result to be compatible with other implementations than commcare
        for (key in bundle.keySet()) {
            result.putExtra(prefix + key, bundle.getString(key))
        }
        activity.setResult(Activity.RESULT_OK, result)
        activity.finish()
    }
}
