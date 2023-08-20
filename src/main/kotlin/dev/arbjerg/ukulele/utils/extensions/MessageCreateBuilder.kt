package dev.arbjerg.ukulele.utils.extensions

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Nonnull
fun MessageCreateBuilder.addCodeBlock(@Nullable text: CharSequence, @Nullable language: CharSequence): MessageCreateBuilder {
    this.addContent("```").addContent(language.toString()).addContent("\n").addContent(text.toString()).addContent("\n```")
    return this
}