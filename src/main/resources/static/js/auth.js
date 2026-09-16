document.querySelectorAll("[data-password-toggle]").forEach((button) => {
    button.addEventListener("click", () => {
        const input = document.getElementById(button.dataset.passwordToggle);

        if (!input) {
            return;
        }

        const isVisible = input.type === "text";
        input.type = isVisible ? "password" : "text";
        button.setAttribute("aria-pressed", String(!isVisible));
        button.setAttribute("aria-label", isVisible ? "Mostrar senha" : "Ocultar senha");

        const icon = button.querySelector(".material-symbols-rounded");
        if (icon) {
            icon.textContent = isVisible ? "visibility" : "visibility_off";
        }
    });
});

const password = document.getElementById("senha");
const passwordConfirmation = document.getElementById("confirmacaoSenha");

function validateMatchingPasswords() {
    if (!password || !passwordConfirmation) {
        return;
    }

    const passwordsDoNotMatch = passwordConfirmation.value !== ""
        && password.value !== passwordConfirmation.value;

    passwordConfirmation.setCustomValidity(
        passwordsDoNotMatch ? "As senhas não coincidem." : ""
    );
}

password?.addEventListener("input", validateMatchingPasswords);
passwordConfirmation?.addEventListener("input", validateMatchingPasswords);
