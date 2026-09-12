(() => {
  "use strict";

  const DEFAULTS = {
    mistEnabled: true,   
    showRules: true,    
    aiOpponents: 2      
  };

  const MIN_NAME = 2;
  const MODES = ["ai", "human", "mixed"];

  const params = new URLSearchParams(location.search);

  const readBool = (value, fallback) =>
    value === null ? fallback : !/^(0|false|no|nao|não)$/i.test(value.trim());

  const readInt = (value, fallback, min, max) => {
    const n = Number.parseInt(value ?? "", 10);
    return Number.isNaN(n) ? fallback : Math.min(max, Math.max(min, n));
  };

  const config = {
    mistEnabled: readBool(params.get("mist"), DEFAULTS.mistEnabled),
    showRules: readBool(params.get("rules"), DEFAULTS.showRules),
    aiOpponents: readInt(params.get("ai"), DEFAULTS.aiOpponents, 1, 4)
  };

  const state = { name: "", mode: null, started: false };

  const el = {
    mist: document.querySelector("[data-mist]"),
    logo: document.querySelector("[data-logo]"),
    logoFallback: document.querySelector("[data-logo-fallback]"),
    name: document.querySelector("[data-name]"),
    modes: document.querySelector("[data-modes]"),
    modeButtons: Array.from(document.querySelectorAll("[data-mode]")),
    play: document.querySelector("[data-play]"),
    hint: document.querySelector("[data-hint]"),
    links: document.querySelector("[data-links]"),
    setup: document.querySelector('[data-view="setup"]'),
    start: document.querySelector('[data-view="start"]'),
    startName: document.querySelector("[data-start-name]"),
    summary: document.querySelector("[data-summary]"),
    back: document.querySelector("[data-back]")
  };


  const isReady = () => state.name.trim().length >= MIN_NAME && state.mode !== null;

  function summaryFor(mode) {
    const n = config.aiOpponents;
    switch (mode) {
      case "ai":
        return n === 1
          ? "Partida contra 1 oponente controlado pela IA."
          : `Partida contra ${n} oponentes controlados pela IA.`;
      case "human":
        return "Partida aberta: aguardando outros jogadores entrarem na mesa.";
      case "mixed":
        return n === 1
          ? "Mesa mista: jogadores humanos completados por 1 IA."
          : `Mesa mista: jogadores humanos completados por ${n} IAs.`;
      default:
        return "";
    }
  }

  function hintFor() {
    const named = state.name.trim().length >= MIN_NAME;
    if (named && state.mode) return "";
    if (!named && state.mode) return "Preencha seu nome para começar.";
    if (named && !state.mode) return "Escolha o formato da partida.";
    return "Preencha seu nome e escolha o formato da partida.";
  }


  function render() {
    el.setup.hidden = state.started;
    el.start.hidden = !state.started;

    el.modeButtons.forEach((button) => {
      const selected = button.dataset.mode === state.mode;
      button.setAttribute("aria-checked", String(selected));
      button.tabIndex = selected ? 0 : -1;
    });

    if (!state.mode && el.modeButtons.length) el.modeButtons[0].tabIndex = 0;

    el.play.disabled = !isReady();
    el.hint.textContent = hintFor();

    el.startName.textContent = state.name.trim();
    el.summary.textContent = summaryFor(state.mode);
  }

  function selectMode(mode) {
    if (!MODES.includes(mode)) return;
    state.mode = mode;
    render();
  }

  function play() {
    if (!isReady()) return;
    state.started = true;
    render();
    el.back.focus();

    document.dispatchEvent(new CustomEvent("carcassonne:start", {
      detail: {
        name: state.name.trim(),
        mode: state.mode,
        aiOpponents: config.aiOpponents
      }
    }));
  }

  function back() {
    state.started = false;
    render();
    el.name.focus();
  }

  el.name.addEventListener("input", (event) => {
    state.name = event.target.value;
    render();
  });

  el.name.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      play();
    }
  });

  el.modeButtons.forEach((button) => {
    button.addEventListener("click", () => selectMode(button.dataset.mode));
  });

  el.modes.addEventListener("keydown", (event) => {
    const step = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[event.key];
    if (!step) return;
    event.preventDefault();
    const current = el.modeButtons.findIndex((b) => b === document.activeElement);
    const next = el.modeButtons[(Math.max(0, current) + step + el.modeButtons.length) % el.modeButtons.length];
    selectMode(next.dataset.mode);
    next.focus();
  });

  el.play.addEventListener("click", play);
  el.back.addEventListener("click", back);

  el.links.addEventListener("click", (event) => {
    const link = event.target.closest("a[data-link]");
    if (link) event.preventDefault();
  });

  if (!config.mistEnabled) el.mist.remove();
  if (!config.showRules) el.links.remove();

  const useLogoFallback = () => {
    el.logo.hidden = true;
    el.logoFallback.hidden = false;
  };

  el.logo.addEventListener("error", useLogoFallback);
  if (el.logo.complete && el.logo.naturalWidth === 0) useLogoFallback();

  const background = new Image();
  background.addEventListener("load", () => {
    document.documentElement.classList.add("has-art");
  });
  background.src = "assets/carcassoneimagemfinal.png";

  render();
})();
