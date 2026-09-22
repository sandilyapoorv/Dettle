package com.dettle.app.domain.model

import kotlinx.serialization.Serializable

// ─── Message ───────────────────────────────────────────────────────────────

enum class MessageRole { USER, ASSISTANT, SYSTEM, TOOL }

enum class MessageType {
    TEXT,           // Normal chat message
    TOOL_CALL,      // AI wants to call a tool
    TOOL_RESULT,    // Result of a tool call
    APPROVAL,       // Waiting for user to approve an action
    SYSTEM,         // System notification
    ERROR,          // Error card
    POLICY_BLOCKED, // PolicyEngine hard-blocked a tool call
    POLICY_WARNING  // PolicyEngine raised a warning (tool still ran)
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val type: MessageType = MessageType.TEXT,
    val content: String,
    val toolCall: ToolCall? = null,
    val toolResult: ToolResult? = null,
    val approvalRequest: ApprovalRequest? = null,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val providerName: String? = null,  // Which AI model produced this
    val tokenCount: Int? = null,
    // Policy enforcement fields
    val policyId: String? = null,
    val policyReason: String? = null,
    val policyFix: String? = null
)

// ─── Tool Calling ──────────────────────────────────────────────────────────

@Serializable
data class ToolCall(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val arguments: Map<String, String> = emptyMap()
)

@Serializable
data class ToolResult(
    val toolCallId: String,
    val toolName: String,
    val content: String,
    val isError: Boolean = false
)

// ─── Approval ──────────────────────────────────────────────────────────────

data class ApprovalRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val actionType: ApprovalActionType,
    val details: Map<String, String> = emptyMap(),
    var status: ApprovalStatus = ApprovalStatus.PENDING
)

enum class ApprovalActionType {
    GITHUB_CREATE_PR,
    GITHUB_PUSH_COMMIT,
    GITHUB_TRIGGER_ACTION,
    CLOUDFLARE_DEPLOY_WORKER,
    CLOUDFLARE_PUBLISH,
    FILE_DELETE,
    CUSTOM
}

enum class ApprovalStatus { PENDING, APPROVED, REJECTED }

// ─── API Format ────────────────────────────────────────────────────────────

/** Simplified message format sent to AI APIs (strips UI-only fields) */
@Serializable
data class ApiMessage(
    val role: String,  // "user", "assistant", "system", "tool"
    val content: String,
    val tool_call_id: String? = null,
    val name: String? = null
)

fun ChatMessage.toApiMessage(): ApiMessage = ApiMessage(
    role = when (role) {
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
        MessageRole.SYSTEM -> "system"
        MessageRole.TOOL -> "tool"
    },
    content = content,
    tool_call_id = toolResult?.toolCallId,
    name = if (role == MessageRole.TOOL) toolResult?.toolName else null
)
