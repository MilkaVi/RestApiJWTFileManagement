package se.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.domain.File;
import se.domain.Message;
import se.exeption.ApiRequestExeption;
import se.repository.MessageRepo;
import se.service.FileService;
import se.service.FileServiceImpl;
import se.service.UserService;
import se.service.UserServiceImpl;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@org.springframework.stereotype.Controller
public class Controller {
    static FileService fileRepository = new FileServiceImpl();
    static UserService userService = new UserServiceImpl();
    @Autowired
    MessageRepo messageRepo;

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    public String distribute(Model model) {
        //distribute to user and admin
        model.addAttribute("messages", messageRepo.findAll());
        if (userService.getUserByUsername(getCurrentUsername()).getRole().equals("USER")) {
            int id = userService.getUserByUsername(getCurrentUsername()).getId();
            model.addAttribute("files", fileRepository.getAllById(id));
            return "user/files";
        } else {
            model.addAttribute("files", fileRepository.getAll());
            model.addAttribute("users", userService.getAll());
            return "admin/files";
        }
    }


    @GetMapping("/files")
    public String getOrderPage(Model model) {
        return distribute(model);
    }


    @GetMapping("/files/new")
    public String addNewOrderPage(Model model) {
        model.addAttribute("doc", new File());
        return "addNewFile";
    }


    @PostMapping("/files")
    public String addNewOrder(
            @RequestParam MultipartFile file1,
            @ModelAttribute File file,
            Errors errors, Model model) throws IOException {

        file.setContent(file1.getBytes());

        if (errors.hasErrors()) {
            return "redirect:/addNewFile";
        }

        int user_id = userService.getUserByUsername(getCurrentUsername()).getId();
        file.setFile_user(user_id);
        fileRepository.save(file);

        return "redirect:/files";

    }


    @PostMapping("/addMess")
    public String addMess(@RequestParam String text, Model model) throws IOException {

        Message mess = new Message();
        mess.setName(getCurrentUsername());
        mess.setText(text);
        messageRepo.save(mess);
        return distribute(model);

    }


    @GetMapping("/files/{files_id}/edit")
    public String edit(@PathVariable("files_id") Integer id,
                       Model model) {
        if (userService.getUserByUsername(getCurrentUsername()).getRole().equals("ADMIN") ||
                userService.getUserByUsername(getCurrentUsername()).getId() == fileRepository.getById(id).getFile_user()) {
            model.addAttribute("doc", fileRepository.getById(id));
            return "update";
        } else
            throw new ApiRequestExeption("\n" +
                    "this file does not belong to you");
    }


    @PatchMapping("/files/{id}")
    public String update(@RequestBody @Valid File file,
                         Errors errors, @PathVariable("id") int id,
                         Model model) {
        if (userService.getUserByUsername(getCurrentUsername()).getId() != fileRepository.getById(id).getFile_user()) {

            throw new ApiRequestExeption("\n" +
                    "this file does not belong to you");
        }

        if (errors.hasErrors()) {
            model.addAttribute("doc", fileRepository.getById(id));
            return "update";
        }

        fileRepository.update(id, file.getUser_id(), file.getName(), file.getDate());

        return "redirect:/files";
    }


    @DeleteMapping("/files/{id}")
    public String delete(@PathVariable("id") int id, Model model) {

        if (userService.getUserByUsername(getCurrentUsername()).getId() != fileRepository.getById(id).getFile_user()) {

            throw new ApiRequestExeption("\n" +
                    "this file does not belong to you");
        }
        fileRepository.delete(id);
        return "redirect:/files";
    }

    @GetMapping("/select")
    public String getOrderFilter(@RequestBody File file,
                                 Model model) {
        int user_id = userService.getUserByUsername(getCurrentUsername()).getId();
        model.addAttribute("messages", messageRepo.findAll());
        if (userService.getUserByUsername(getCurrentUsername()).getRole().equals("USER")) {
            model.addAttribute("files", fileRepository.select(0, file.getUser_id(), file.getName(), file.getDate()));
            model.addAttribute("users", userService.getAll());
            return "admin/files";
        } else {
            model.addAttribute("files", fileRepository.select(user_id, file.getUser_id(), file.getName(), file.getDate()));
            return "user/files";
        }

    }

    @GetMapping("/sort")
    public String sort(@RequestBody String field, Model model) {
        String fieldNew = field.replaceAll("\"", "");
        if (userService.getUserByUsername(getCurrentUsername()).getRole().equals("USER")) {
            throw new ApiRequestExeption("\n" +
                    "this file does not belong to you");
        }
        model.addAttribute("messages", messageRepo.findAll());
        model.addAttribute("files", fileRepository.sort(0, fieldNew));
        model.addAttribute("users", userService.getAll());

        return "admin/files";
    }

    @GetMapping("/download/{id}")
    public void downloadFile(@PathVariable("id") int id, HttpServletResponse response) throws Exception {

        File file = fileRepository.getById(id);

        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + file.getName();

        response.setHeader(headerKey, headerValue);

        ServletOutputStream outputStream = response.getOutputStream();

        outputStream.write(file.getContent());
        outputStream.close();
    }

}
