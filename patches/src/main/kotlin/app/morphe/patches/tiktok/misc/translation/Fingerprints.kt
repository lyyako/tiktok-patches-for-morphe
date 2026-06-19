package app.morphe.patches.tiktok.misc.translation

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object MultiCommentTranslationStartFingerprint : Fingerprint(
    definingClass = "LX/0Pwq;",
    name = "LJFF",
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "LX/0QML;", "Z"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<MethodReference>()?.let { reference ->
                reference.definingClass == "Lcom/ss/android/ugc/aweme/comment/translation/CommentMultiTranslationApi\$RealApi;" &&
                    reference.name == "getMultiTranslation"
            } == true
        } == true
    },
)

internal object BaseCommentCellBindFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
    name = "g7",
    returnType = "V",
    parameters = listOf("LX/0srE;"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.let { reference ->
                reference.definingClass == "LX/0QMJ;" &&
                    reference.name == "LLILZ" &&
                    reference.type == "LX/0QML;"
            } == true
        } == true
    },
)

internal object CommentListLoadedFingerprint : Fingerprint(
    definingClass = "LX/0sqp;",
    name = "LJIJJLI",
    returnType = "V",
    parameters = listOf(
        "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;",
        "Z",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "LX/0NJN;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "LX/02vj;",
        "I",
        "I",
    ),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.let { reference ->
                reference.definingClass == "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;" &&
                    reference.name == "items" &&
                    reference.type == "Ljava/util/List;"
            } == true
        } == true
    },
)

internal object MultiCommentTranslationCompleteFingerprint : Fingerprint(
    definingClass = "LX/0QfV;",
    name = "run\$2",
    returnType = "V",
    parameters = listOf("LX/0QfV;"),
    strings = listOf("MultiCommentTranslationTask startTranslate onComplete "),
)
