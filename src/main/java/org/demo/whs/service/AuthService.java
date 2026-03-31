package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Auth.ChangePassWordRequest;
import org.demo.whs.entity.dto.request.Auth.RegisterRequest;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.Auth.ForgotPasswordResponse;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.Permission.MyPermissionsResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.ActionType;

/**
 * Service Interface for managing authentication and authorization.
 */
public interface AuthService {

    /**
     * Authenticates a user based on the provided login request.
     *
     * @param request the login request containing user credentials
     * @param clientIp the client IP address
     * @return an authentication response containing tokens and related information
     */
    AuthResponse authenticate(LoginRequest request, String clientIp);

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request the refresh token request
     * @return a new access token and its expiration
     */
    RefreshTokenResponse refreshToken(RefreshTokenRequest request);
    /**
     * Registers a new user based on the provided registration request.
     *
     * @param request the registration request containing user details
     */
    void register(RegisterRequest request);

    /**
     * Sends a forgot password OTP to the provided email address.
     *
     * @param email the email address to send the OTP to
     */
    void forgotPassword(String email);

    /**
     * Verifies the forgot password OTP and returns a password reset response.
     *
     * @param email the email address
     * @param otp the OTP to verify
     * @return a ForgotPasswordResponse containing JWT if valid
     */
    ForgotPasswordResponse verifyForgotPasswordOtp(String email, String otp);

    /**
     * Resets the user's password using the current authenticated session and its token.
     *
     * @param authHeader the Authorization header containing the JWT
     * @param newPassword the new password
     */
    void resetPassword(String authHeader, String newPassword);

    /**
     * Changes the user's password using the current authenticated session and its token.
     *
     * @param request the change password request containing old and new passwords
     */
    void changePassword(ChangePassWordRequest request);

    /**
     * Kiểm tra user hiện tại có permission hay không.
     *
     * @param resource resource (USER, ORDER...)
     * @param action action (CREATE, UPDATE...)
     * @return true nếu có quyền
     */
    boolean checkPermission(String resource, ActionType action);

    /**
     * Lấy danh sách permission của user hiện tại.
     *
     * @return MyPermissionsResponse chứa danh sách permission
     */
    MyPermissionsResponse getMyPermissions();
}
