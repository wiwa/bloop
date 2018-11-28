const React = require("react");

const CompLibrary = require("../../core/CompLibrary.js");
const translate = require("../../server/translate.js").translate;
const Container = CompLibrary.Container;
const MarkdownBlock = CompLibrary.MarkdownBlock;

// Import paths to markdown files of the setup
const siteConfig = require(process.cwd() + "/siteConfig.js");
const toolsMD = siteConfig.toolsMD;

const SetupHeader = () => {
  return (
    <div className="page-header text-center">
      <h1>
        <translate desc="setup page - header">Installing Bloop</translate>
      </h1>
      <p>
        <translate desc="setup page - header desc">
          How to set up Bloop with your tool of choice.
        </translate>
      </p>
    </div>
  );
};

const SetupSelectButton = props => {
  const showTools = Object.keys(props.types.items).map((tool, j) => {
    return (
      <a
        key={j}
        data-title={tool}
        href="#installation"
        className="tools-button"
      >
        {props.types.items[tool]}
      </a>
    );
  });
  return (
    <div className="tools-group">
      <h5>{props.types.name}</h5>
      {showTools}
    </div>
  );
};

const SetupOptions = () => {
  const tools = siteConfig.tools;
  const showCase = tools.map((types, i) => {
    return <SetupSelectButton key={i} types={types} />;
  });
  return (
    <div className="step-setup">
      <h2>
        <span className="step-no">1</span>
        <translate desc="setup page - step 1">
          Pick your preferred method
        </translate>
      </h2>
      {showCase}
    </div>
  );
};

const StepInstallAndUsage = props => {
  const markdownsElement = toolsMD.map((tool, index) => (
    <div className="items" data-title={tool.title} key={index}>
      <MarkdownBlock key={index}>{tool[props.name]}</MarkdownBlock>
    </div>
  ));
  const installation = (
    <translate desc="setup page - step 2">Installation</translate>
  );
  const usage = <translate desc="setup page - step 3">Usage</translate>;
  return (
    <div className="step-hidden step-setup">
      <h2 id={props.name === "install" ? "installation" : ""}>
        <span className="step-no">{props.number}</span>
        {props.name === "install" ? installation : usage}
      </h2>
      {markdownsElement}
    </div>
  );
};

const StepFour = () => {
  return (
    <div className="step-hidden step-setup">
      <h2>
        <span className="step-no">4</span>
        <translate desc="setup page - step 4 one">Generate bloop configuration files</translate>
      </h2>
      <MarkdownBlock>
        Great! Bloop has already been installed and the build server is running in the background.
      </MarkdownBlock>
      <MarkdownBlock>
        To start compiling, testing and running your projects, you need to generate Bloop JSON
        configuration files from your build. Head to the [Generating configuration files]()
        section to learn how to set up your build to export your project.
      </MarkdownBlock>
    </div>
  );
};

const SetupContent = () => {
  return (
    <Container padding={["bottom"]}>
      <div className="step">
        <SetupOptions />
        <StepInstallAndUsage name="install" number="2" />
        <StepInstallAndUsage name="usage" number="3" />
        <StepFour />
      </div>
    </Container>
  );
};

class Setup extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
    const time = new Date().getTime();
    return (
      <div className="mainContainer">
        <div className="installationContainer">
          <SetupHeader />
          <SetupContent />
          <script src={`${siteConfig.baseUrl}scripts/tools.js?t=${time}`} />
        </div>
      </div>
    );
  }
}

module.exports = Setup;
