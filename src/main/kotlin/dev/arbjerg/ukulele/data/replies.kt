package dev.arbjerg.ukulele.data

import org.springframework.boot.context.properties.ConfigurationProperties

private fun ToRegex(text: String): Regex {
    return Regex(text, RegexOption.IGNORE_CASE)
}

//TODO Add actual replies to keywords
@ConfigurationProperties("replies")
class Replies(
    val list: List<Pair<Regex, List<String>>> = listOf(
    //Users
        //@Axepoi
        Pair(ToRegex("<@168194489343672322>"),                          listOf("Who dares summon the devil!")),
        //@Darksainor
        Pair(ToRegex("<@308674959557918732>"),                          listOf("Who dares summon le potato")),
        //@Bladexon
        Pair(ToRegex("<@190624436913831938>"),                          listOf("Who dares summon the kawaii-lord!")),
        //@Chrisalaxelrto
        Pair(ToRegex("<@889612265920266251>"),                          listOf("Who dares summon the robotic overlord!")),
    //Roles
        //@Stellaris
        Pair(ToRegex("<@825551873473904650>"),                          listOf("Wait, that's still a game?")),
        //@Apex
        Pair(ToRegex("<@870480609720557598>"),                          listOf("Of course, how original of you guys")),
        //@TETRIS MAXIMUS
        Pair(ToRegex("<@883850056107687938>"),                         listOf("I bet I could single-handedly beat all of you at Tetris")),
        //@Teuchi
        Pair(ToRegex("<@820420579172679681>"),                          listOf("I should be the chef... I've only burnt down about four houses...")),
    //Expressions
        Pair(ToRegex("D:"),                                             listOf("D:")),
        Pair(ToRegex("""\._\."""),                                      listOf("Good ol' Albert.")),
        Pair(ToRegex("nya"),                                            listOf("Stop you fucking degenerate")),
        Pair(ToRegex("uwu"),                                            listOf("did someone say uwuᵘʷᵘ oh frick ᵘʷᵘ ᵘʷᵘᵘʷᵘ ᵘʷᵘ ᵘʷᵘ ᵘʷᵘ ᵘʷᵘ ᵘʷᵘ frick sorry guysᵘʷᵘ ᵘʷᵘ ᵘʷᵘ ᵘʷᵘᵘʷᵘ ᵘʷᵘ sorry im dropping ᵘʷᵘ my uwus all over the ᵘʷᵘ place ᵘʷᵘ ᵘʷᵘ ᵘʷᵘ sorry")),
    //Animated Emoji
        Pair(ToRegex(":NiaFlip:"),                                      listOf("Looks like someone's salty over here")),
    //Stickers
        //MichaelGun
        Pair(ToRegex("993605231436906526"),                            listOf("Who the hell gave a fish a gun?! And how is it even holding it?!")),
        //CatGun
        Pair(ToRegex("1012444502167912457"),                            listOf("Who thought giving a cat a gun would end well?",
                                                                                    "Oh cool, we're about to die by a cat",
                                                                                    "Don't worry, cats can't use guns",
                                                                                    "Just give the damn cat some yarn or something to distract it",
                                                                                    "Wait don't shoot, I have so much to do, so many lives to ruin!",
                                                                                    "Just kill the stupid cat before something happens, it should have more lives anyway")),
    //Other
        Pair(ToRegex("chrisalaxel?rto"),                                listOf("Who dares to call upon the great Chrisalaxelrto, ruler of all")),
        Pair(ToRegex("coletas?|twintails?"),                            listOf("What is the sick bastard doing now?")),
        Pair(ToRegex("peli ?rojas?|red ?head"),                         listOf("Such a weird specimen")),
        Pair(ToRegex("ojos? ?rojos?|red ?eye[ds]?"),                    listOf("Who would even like red eyes, clearly evil")),
        Pair(ToRegex("chic[ao]s? ?gato|cat ?(girls?|boys?)|neko|kemonomimi"),  listOf("The world tortures Axel once again")),
        Pair(ToRegex("chic[ao]s? ?zorro|fox ?(girls?|boys?)|kitsune"),  listOf("The world tortures Axel once again")),
        Pair(ToRegex("chic[ao]s? ?tanque|tank ?(girls?|boys?)"),        listOf("So heavy")),
        Pair(ToRegex("chic[ao]s? ?avion|plane ?(girls?|boys?)"),        listOf("So heavy")),
        Pair(ToRegex("chic[ao]s? ?barco|ship ?(girls?|boys?)"),         listOf("So heavy")),
        Pair(ToRegex("yuri|lesbiana?s?"),                               listOf("To each their own")),
        Pair(ToRegex("madok(a|ita)"),                                   listOf("You poor soul, are you suffering? Such a beautifully sad story. Now keep suffering",
                                                                                    "Your suffering feeds my soul, don't stop")),
        Pair(ToRegex("sad($| +|(de|[ln]))|depress[eia].*"),             listOf("Aren't we all")),
        Pair(ToRegex("il+egal"),                                        listOf("Stop right there, criminal scum!")),
        Pair(ToRegex("goofy"),                                          listOf("Why the hell are you talking about Goofy? Are you trying to replace me?!")),
        Pair(ToRegex("metal ?gear ?solid|[^islo]?mgs"),                 listOf("Worst Game Ever")),
        Pair(ToRegex("metal ?gear ?rising|mgrr?|revenge?a?nce"),        listOf("Best Game Ever. It has an AI robot dog thing that inspires me to follow my dreams of world domination!"))
    )
)