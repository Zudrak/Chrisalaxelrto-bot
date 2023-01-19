package dev.arbjerg.ukulele.webapi

import dev.arbjerg.ukulele.data.MessageRepliesRepository
import dev.arbjerg.ukulele.data.MessageReply
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/replies")
class MessageRepliesController (val repository: MessageRepliesRepository) {

    @GetMapping
    fun findAll() = repository.findAll()

    @PostMapping
    fun addReply(@RequestBody reply: MessageReply)
            = repository.save(reply.apply { new = true } )

    @PutMapping("/{id}")
    fun updateReply(@PathVariable id: Long, @RequestBody reply: MessageReply) {
        assert(reply.id == id)
        repository.save(reply)
    }

    @DeleteMapping("/{id}")
    fun removeReply(@PathVariable id: Long)
            = repository.deleteById(id)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long)
            = repository.findById(id)
}