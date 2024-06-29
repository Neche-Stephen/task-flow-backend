package com.squad22podA.task_mgt.service.impl;

import com.squad22podA.task_mgt.config.JwtService;
import com.squad22podA.task_mgt.entity.enums.Role;
import com.squad22podA.task_mgt.entity.model.ConfirmationToken;
import com.squad22podA.task_mgt.entity.model.UserModel;
import com.squad22podA.task_mgt.exception.EmailAlreadyExistsException;
import com.squad22podA.task_mgt.payload.request.*;
import com.squad22podA.task_mgt.repository.ConfirmationTokenRepository;
import com.squad22podA.task_mgt.repository.UserModelRepository;
import com.squad22podA.task_mgt.service.EmailService;
import com.squad22podA.task_mgt.service.UserModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class UserModelServiceImpl implements UserModelService {


    private final UserModelRepository userModelRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailService emailService;

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;



    @Override
    public String registerUser(UserRegistrationRequest registrationRequest) {


        // Validate email format
        String emailRegex = "^(.+)@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(registrationRequest.getEmail());

        if(!matcher.matches()){
            return "Invalid Email";
        }

//        if (!matcher.matches()) {
//            return RegisterResponse.builder()
//                    .responseCode(UserUtils.INVALID_EMAIL_FORMAT_CODE)
//                    .responseMessage(UserUtils.INVALID_EMAIL_FORMAT_MESSAGE)
//                    .build();
//        }

// Validate email domain
        String[] emailParts = registrationRequest.getEmail().split("\\.");
        if (emailParts.length < 2 || emailParts[emailParts.length - 1].length() < 2) {
            System.out.println("Invalid email domain. Email parts: " + Arrays.toString(emailParts));
            return "Invalid Email domain";

//            return RegisterResponse.builder()
//                    .responseCode(UserUtils.INVALID_EMAIL_DOMAIN_CODE)
//                    .responseMessage(UserUtils.INVALID_EMAIL_DOMAIN_MESSAGE)
//                    .build();
        }


        if(!registrationRequest.getPassword().equals(registrationRequest.getConfirmPassword())){
            throw new IllegalArgumentException("Passwords do not match!");
        }

        Optional<UserModel> existingUser = userModelRepository.findByEmail(registrationRequest.getEmail());

        if(existingUser.isPresent()){
            throw new EmailAlreadyExistsException("Email already exists. Login to your account");
        }


        UserModel newUser = UserModel.builder().firstName(registrationRequest.getFirstName())
                                        .lastName(registrationRequest.getLastName())
                                        .email(registrationRequest.getEmail())
                                        .phoneNumber(registrationRequest.getPhoneNumber())
                                        .password(passwordEncoder.encode(registrationRequest.getPassword()))
                                        .role(Role.USER)
                                        .build();

        UserModel savedUser = userModelRepository.save(newUser);

        ConfirmationToken confirmationToken = new ConfirmationToken(savedUser);
        confirmationTokenRepository.save(confirmationToken);

        //String confirmationUrl = "http://localhost:8080/api/auth/confirm?token=" + confirmationToken.getToken();
        String confirmationUrl = "http://127.0.0.1:5500/confirmation/confirm-token-sucess.html?token=" + confirmationToken.getToken();

        //send email alert
        EmailDetails emailDetails = EmailDetails.builder()
                                    .recipient(savedUser.getEmail())
                                    .subject("ACCOUNT CREATION")
                                    .messageBody("CONGRATULATIONS!!! Your User Account Has Been Successfully Created.\n"
                                    + "Your Account Details: \n" + "Account FullName: " + savedUser.getFirstName() + " \n"
                                     + "Confirm your email " +
                                            "Please click the link to confirm your registration: " + confirmationUrl)
                                    .build();

        emailService.sendEmailAlert(emailDetails);
        return "Confirmed Email!!!";

    }

    @Override
    public LoginResponse loginUser(LoginRequestDto loginRequestDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getEmail(),
                        loginRequestDto.getPassword()
                )
        );
        UserModel user = userModelRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(user);

        return LoginResponse.builder()
                .responseCode("002")
                .responseMessage("Login Successfully")
                .loginInfo(LoginInfo.builder()
                        .email(user.getEmail())
                        .token(jwtToken)
                        .build())
                .build();
    }


}
