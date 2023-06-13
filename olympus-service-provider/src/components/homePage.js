import React from "react";
import { connect } from "react-redux";
import FrontPage from "./frontPage";

function HomePage(props) {
	return <FrontPage />;
}

function mapStateToProps(state) {
  return {
    user: state.oidc.user,
  };
}

function mapDispatchToProps(dispatch) {
  return {
    dispatch,
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(HomePage);
