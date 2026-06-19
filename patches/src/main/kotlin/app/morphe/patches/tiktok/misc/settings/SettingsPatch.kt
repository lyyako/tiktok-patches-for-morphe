/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/settings/SettingsPatch.kt
 */
package app.morphe.patches.tiktok.misc.settings

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method as SmaliMethod
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/settings/TikTokActivityHook;"
private const val SETTINGS_ACTIVITY_DESCRIPTOR =
    "Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;"
private const val ANDROID_CONTEXT_GET_STRING = "Landroid/content/Context;->getString(I)Ljava/lang/String;"
private const val SETTINGS_ACTION = "morphe_settings"
private const val SETTINGS_TITLE = "Morphe settings"
private const val SETTINGS_ICON_ID = "0x7f010088"

private data class OpenDebugTargets(
    val stateClass: String,
    val composeMethod: MutableMethod,
)

@Suppress("unused")
val settingsPatch = bytecodePatch(
    name = "Settings",
    description = "Adds Morphe settings to TikTok through the hidden Open Debug settings row. Supports TikTok 43.8.3.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(*AppCompatibilities.tiktok4383())

    execute {
        val initializeSettingsMethodDescriptor = "$EXTENSION_CLASS_DESCRIPTOR->initialize($SETTINGS_ACTIVITY_DESCRIPTOR)Z"

        fun isOpenDebugRowCompose(method: SmaliMethod, stateClass: String): Boolean {
            val implementation = method.implementation ?: return false
            val readsOpenDebugState = implementation.instructions.any { instruction ->
                if (instruction.opcode != Opcode.IGET_OBJECT) return@any false
                val fieldReference = instruction.getReference<FieldReference>() ?: return@any false
                fieldReference.definingClass == stateClass
            }
            val readsTitleString = implementation.instructions.any { instruction ->
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    instruction.getReference<MethodReference>()?.toString() == ANDROID_CONTEXT_GET_STRING
            }

            return readsOpenDebugState && readsTitleString
        }

        fun resolveOpenDebugTargets(): OpenDebugTargets {
            val defaultStateMethod = OpenDebugCellVmDefaultStateFingerprint.method
            val stateClass = defaultStateMethod.implementation?.instructions?.firstNotNullOfOrNull { instruction ->
                if (instruction.opcode != Opcode.NEW_INSTANCE) return@firstNotNullOfOrNull null
                instruction.getReference<TypeReference>()?.type
            } ?: throw PatchException("Settings: could not resolve OpenDebug state class.")

            val matches = mutableListOf<Pair<ClassDef, SmaliMethod>>()
            classDefForEach { classDef ->
                classDef.methods.forEach { method ->
                    val parameters = method.parameterTypes
                    if (method.name != "LIZ" || method.returnType != "V") return@forEach
                    if (parameters.size != 5) return@forEach
                    if (parameters[0] != stateClass || parameters[1] != "Z" || parameters[2] != "Z") return@forEach
                    if (!parameters[3].startsWith("LX/") || parameters[4] != "I") return@forEach
                    if (!isOpenDebugRowCompose(method, stateClass)) return@forEach

                    matches += classDef to method
                }
            }

            if (matches.size != 1) {
                throw PatchException("Settings: expected one OpenDebug row compose method, found ${matches.size}.")
            }

            val (composeClass, composeMethod) = matches.single()
            return OpenDebugTargets(
                stateClass = stateClass,
                composeMethod = mutableClassDefBy(composeClass).findMutableMethodOf(composeMethod),
            )
        }

        fun SmaliMethod.clickLambdaScore(wrapperClass: String, stateClass: String): Int {
            val implementation = implementation ?: return 0
            var score = 0

            implementation.instructions.forEach { instruction ->
                val reference = instruction.getReference<Reference>()

                if (instruction.opcode == Opcode.IGET_OBJECT) {
                    val field = reference as? FieldReference ?: return@forEach
                    if (field.definingClass == wrapperClass && field.name == "l1") score += 25
                    if (field.definingClass == wrapperClass && field.name == "l0") score += 15
                }

                if (instruction.opcode == Opcode.CHECK_CAST) {
                    val type = reference as? TypeReference ?: return@forEach
                    if (type.type == stateClass) score += 40
                    if (type.type == "Landroid/content/Context;") score += 10
                }
            }

            return score
        }

        fun MutableMethod.findClickWrapperClassAndInvokeName(stateClass: String): Pair<String, String> {
            val instructions = implementation!!.instructions.toList()

            val wrapperClass = instructions.withIndex().firstNotNullOfOrNull { (index, instruction) ->
                if (instruction.opcode != Opcode.INVOKE_DIRECT) return@firstNotNullOfOrNull null
                val invokeDirect = instruction as? Instruction35c ?: return@firstNotNullOfOrNull null
                val reference = invokeDirect.reference as? MethodReference ?: return@firstNotNullOfOrNull null

                if (!reference.definingClass.startsWith("Lkotlin/jvm/internal/AwS")) return@firstNotNullOfOrNull null
                if (reference.parameterTypes != listOf(stateClass, "Landroid/content/Context;", "I")) {
                    return@firstNotNullOfOrNull null
                }

                val discriminatorRegister = when (invokeDirect.registerCount) {
                    4 -> invokeDirect.registerF
                    5 -> invokeDirect.registerG
                    else -> throw PatchException(
                        "Settings: unexpected OpenDebug click wrapper constructor register count ${invokeDirect.registerCount}.",
                    )
                }

                val discriminator = instructions.take(index).asReversed().firstNotNullOfOrNull { previous ->
                    val register = (previous as? OneRegisterInstruction)?.registerA
                        ?: return@firstNotNullOfOrNull null
                    if (register != discriminatorRegister) return@firstNotNullOfOrNull null
                    (previous as? NarrowLiteralInstruction)?.narrowLiteral
                } ?: throw PatchException("Settings: could not resolve OpenDebug click wrapper discriminator.")

                reference.definingClass to "invoke\$$discriminator"
            } ?: throw PatchException("Settings: could not resolve OpenDebug click wrapper class.")

            return wrapperClass
        }

        fun resolveClickWrapperMethod(wrapperClass: String, invokeName: String, stateClass: String): MutableMethod {
            val matches = mutableListOf<MutableMethod>()
            classDefForEach { classDef ->
                if (classDef.type != wrapperClass) return@classDefForEach

                classDef.methods.forEach { method ->
                    if (method.name != invokeName) return@forEach
                    if (method.parameterTypes != listOf(classDef.type)) return@forEach
                    if (method.clickLambdaScore(classDef.type, stateClass) < 70) return@forEach

                    matches += mutableClassDefBy(classDef).findMutableMethodOf(method)
                }
            }

            if (matches.size != 1) {
                throw PatchException("Settings: expected one OpenDebug click handler in $wrapperClass, found ${matches.size}.")
            }

            return matches.single()
        }

        fun resolveOpenDebugFunction2Method(): MutableMethod {
            val defaultStateMethod = OpenDebugCellVmDefaultStateFingerprint.method
            val openDebugViewModelClass = defaultStateMethod.definingClass
            val lambdaClass = defaultStateMethod.implementation?.instructions?.firstNotNullOfOrNull { instruction ->
                if (instruction.opcode != Opcode.INVOKE_DIRECT) return@firstNotNullOfOrNull null
                val reference = instruction.getReference<MethodReference>() ?: return@firstNotNullOfOrNull null
                if (!reference.definingClass.startsWith("Lkotlin/jvm/internal/AwS")) return@firstNotNullOfOrNull null
                if (reference.parameterTypes.firstOrNull() != openDebugViewModelClass) return@firstNotNullOfOrNull null

                reference.definingClass
            } ?: throw PatchException("Settings: could not resolve OpenDebug Function2 lambda class.")

            val matches = mutableListOf<MutableMethod>()
            classDefForEach { classDef ->
                if (classDef.type != lambdaClass) return@classDefForEach

                classDef.methods.forEach { method ->
                    if (!method.name.matches(Regex("invoke\\\$\\d+"))) return@forEach
                    if (method.returnType != "Ljava/lang/Object;") return@forEach
                    if (method.parameterTypes.size != 3 || method.parameterTypes[0] != lambdaClass) return@forEach

                    val castsOpenDebugViewModel = method.implementation?.instructions?.any { instruction ->
                        instruction.opcode == Opcode.CHECK_CAST &&
                            instruction.getReference<TypeReference>()?.type == openDebugViewModelClass
                    } == true
                    if (!castsOpenDebugViewModel) return@forEach

                    matches += mutableClassDefBy(classDef).findMutableMethodOf(method)
                }
            }

            if (matches.size != 1) {
                throw PatchException("Settings: expected one OpenDebug Function2 lambda, found ${matches.size}.")
            }

            return matches.single()
        }

        fun MutableMethod.openMorpheSettingsAtStart(contextRegister: String) {
            addInstructions(
                0,
                """
                    invoke-static {}, Lapp/morphe/extension/shared/Utils;->getContext()Landroid/content/Context;
                    move-result-object v$contextRegister
                    if-eqz v$contextRegister, :return_unit
                    new-instance v1, Landroid/content/Intent;
                    const-class v2, $SETTINGS_ACTIVITY_DESCRIPTOR
                    invoke-direct {v1, v$contextRegister, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                    const/high16 v2, 0x10000000
                    invoke-virtual {v1, v2}, Landroid/content/Intent;->setFlags(I)Landroid/content/Intent;
                    const-string v2, "$SETTINGS_ACTION"
                    invoke-virtual {v1, v2}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;
                    invoke-virtual {v$contextRegister, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                    :return_unit
                    sget-object v$contextRegister, Lkotlin/Unit;->LIZ:Lkotlin/Unit;
                    return-object v$contextRegister
                """,
            )
        }

        fun addOpenDebugToVisibleSettingsList(): Boolean {
            val composeRowsMethod = SettingsComposeRowsFingerprint.methodOrNull ?: return false
            val openDebugField = SupportGroupDefaultStateFingerprint.method.implementation?.instructions
                ?.firstNotNullOfOrNull { instruction ->
                    if (instruction.opcode != Opcode.SGET_OBJECT) return@firstNotNullOfOrNull null
                    val field = instruction.getReference<FieldReference>() ?: return@firstNotNullOfOrNull null
                    field.takeIf { it.name == "SECTION_HEADER" }
                } ?: return false

            val sortedListIndex = composeRowsMethod.implementation?.instructions?.indexOfLast { instruction ->
                if (instruction.opcode != Opcode.INVOKE_STATIC) return@indexOfLast false
                val reference = instruction.getReference<MethodReference>() ?: return@indexOfLast false
                reference.name == "LJLJLLL" &&
                    reference.parameterTypes == listOf("Ljava/util/Comparator;", "Ljava/lang/Iterable;") &&
                    reference.returnType == "Ljava/util/List;"
            } ?: -1
            if (sortedListIndex < 0) return false

            val listRegister = (composeRowsMethod.getInstruction(sortedListIndex + 1) as? OneRegisterInstruction)
                ?.registerA ?: return false

            composeRowsMethod.addInstructions(
                sortedListIndex + 2,
                """
                    new-instance v0, Ljava/util/ArrayList;
                    invoke-direct {v0, v$listRegister}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                    sget-object v1, ${openDebugField.definingClass}->OPEN_DEBUG:${openDebugField.type}
                    const/4 v2, 0x0
                    invoke-virtual {v0, v2, v1}, Ljava/util/ArrayList;->add(ILjava/lang/Object;)V
                    move-object v$listRegister, v0
                """,
            )

            return true
        }

        fun addOpenDebugToSupportGroupDefaultState() {
            SupportGroupDefaultStateFingerprint.method.apply {
                val sectionHeaderSgetIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.SGET_OBJECT && getReference<FieldReference>()?.name == "SECTION_HEADER"
                }
                val sectionHeaderField = getInstruction<ReferenceInstruction>(sectionHeaderSgetIndex).reference as FieldReference
                val addInstruction = getInstruction<Instruction35c>(sectionHeaderSgetIndex + 1)
                val addReference = addInstruction.reference as MethodReference
                val listRegister = addInstruction.registerC
                val itemRegister = addInstruction.registerD

                addInstructions(
                    sectionHeaderSgetIndex + 2,
                    """
                        sget-object v$itemRegister, ${sectionHeaderField.definingClass}->OPEN_DEBUG:${sectionHeaderField.type}
                        invoke-virtual {v$listRegister, v$itemRegister}, $addReference
                    """,
                )
            }
        }

        fun patchSettingsActivityLaunch() {
            val activityOnCreate = AdPersonalizationActivityOnCreateFingerprint.method
            val implementation = activityOnCreate.implementation!!
            val initializeSettingsIndex = implementation.instructions.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER } + 1
            if (initializeSettingsIndex <= 0) {
                throw PatchException("Settings: could not locate AdPersonalizationActivity onCreate invoke-super.")
            }

            val thisRegister = activityOnCreate.getInstruction<Instruction35c>(initializeSettingsIndex - 1).registerC
            val usableRegister = implementation.registerCount - activityOnCreate.parameterTypes.size - 2

            activityOnCreate.addInstructionsWithLabels(
                initializeSettingsIndex,
                """
                    invoke-static {v$thisRegister}, $initializeSettingsMethodDescriptor
                    move-result v$usableRegister
                    if-eqz v$usableRegister, :do_not_open
                    return-void
                """,
                ExternalLabel("do_not_open", activityOnCreate.getInstruction(initializeSettingsIndex)),
            )
        }

        fun patchOpenDebugTitle(composeMethod: MutableMethod) {
            val getStringInvokeIndex = composeMethod.indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.toString() == ANDROID_CONTEXT_GET_STRING
            }
            val moveResultIndex = getStringInvokeIndex + 1
            val titleRegister = composeMethod.getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

            composeMethod.addInstruction(moveResultIndex + 1, "const-string v$titleRegister, \"$SETTINGS_TITLE\"")
        }

        fun patchOpenDebugIcon() {
            OpenDebugCellVmDefaultStateFingerprint.method.apply {
                val iconIdLiteralIndex = implementation?.instructions?.indexOfFirst { instruction ->
                    instruction is NarrowLiteralInstruction &&
                        (instruction.narrowLiteral == 0x7f0107e3 || instruction.narrowLiteral == 0x7f0107e7)
                } ?: -1

                if (iconIdLiteralIndex >= 0) {
                    val iconRegister = getInstruction<OneRegisterInstruction>(iconIdLiteralIndex).registerA
                    replaceInstruction(iconIdLiteralIndex, "const v$iconRegister, $SETTINGS_ICON_ID")
                }
            }
        }

        fun MutableMethod.openMorpheSettingsFromContextField(wrapperClass: String) {
            addInstructions(
                0,
                """
                    iget-object v0, p0, $wrapperClass->l1:Ljava/lang/Object;
                    check-cast v0, Landroid/content/Context;
                    new-instance v1, Landroid/content/Intent;
                    const-class v2, $SETTINGS_ACTIVITY_DESCRIPTOR
                    invoke-direct {v1, v0, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                    const-string v2, "$SETTINGS_ACTION"
                    invoke-virtual {v1, v2}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;
                    const/high16 v2, 0x10000000
                    invoke-virtual {v1, v2}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
                    invoke-virtual {v0, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                    sget-object v0, Lkotlin/Unit;->LIZ:Lkotlin/Unit;
                    return-object v0
                """,
            )
        }

        val openDebugTargets = resolveOpenDebugTargets()

        if (!addOpenDebugToVisibleSettingsList()) {
            addOpenDebugToSupportGroupDefaultState()
        }

        patchSettingsActivityLaunch()
        patchOpenDebugTitle(openDebugTargets.composeMethod)
        patchOpenDebugIcon()

        val (wrapperClass, wrapperInvokeName) = openDebugTargets.composeMethod
            .findClickWrapperClassAndInvokeName(openDebugTargets.stateClass)
        resolveClickWrapperMethod(wrapperClass, wrapperInvokeName, openDebugTargets.stateClass)
            .openMorpheSettingsFromContextField(wrapperClass)
        resolveOpenDebugFunction2Method().openMorpheSettingsAtStart("0")
    }
}

