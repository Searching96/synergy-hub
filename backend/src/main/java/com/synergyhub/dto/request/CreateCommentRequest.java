package com.synergyhub.dto.request;

import com.synergyhub.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "Comment content cannot be empty")
    @Size(max = 1000, message = "Comment content must not exceed 1000 characters")
    @NoHtml(allowFormatting = true, message = "Comment cannot contain HTML tags")
    private String content;
}