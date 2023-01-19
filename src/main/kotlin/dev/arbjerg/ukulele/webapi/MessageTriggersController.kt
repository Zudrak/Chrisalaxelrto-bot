package dev.arbjerg.ukulele.webapi

import dev.arbjerg.ukulele.data.MessageTrigger
import dev.arbjerg.ukulele.data.MessageTriggersRepository
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/triggers")
class MessageTriggersController (val repository: MessageTriggersRepository) {

    @GetMapping
    fun findAll() = repository.findAll()

    @PostMapping
    fun addTrigger(@RequestBody trigger: MessageTrigger)
            = repository.save(trigger.apply { new = true } )

    @PutMapping("/{id}")
    fun updateTrigger(@PathVariable id: Long, @RequestBody trigger: MessageTrigger)
        = repository.save(trigger)


    @DeleteMapping("/{id}")
    fun removeTrigger(@PathVariable id: Long)
            = repository.deleteById(id)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long)
            = repository.findById(id)
}